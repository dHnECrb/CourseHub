package com.example.springdemo.manager;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnlineUserManagerTests {
    private static final String SESSION_ID = "session-1";
    private static final String USER_ID = "12";
    private static final String HEARTBEAT_KEY = "online:heartbeat:" + SESSION_ID;
    private static final String SESSION_USERS_KEY = "online:sessionUsers";
    private static final String LAST_ACTIVE_KEY = "online:lastActive";
    private static final String USER_SESSIONS_KEY = "online:user:" + USER_ID;
    private static final String LIMITS_KEY = "online:limits";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private MessageSource msgSource;

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private OnlineUserManager onlineUserManager;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(httpSession.getId()).thenReturn(SESSION_ID);
        lenient().when(httpSession.getAttribute("userId")).thenReturn(USER_ID);
        lenient().when(msgSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void loginStoresSessionAndSetsHeartbeatTtl() {
        onlineUserManager.login(httpSession);

        verify(valueOperations).set(eq(HEARTBEAT_KEY), anyString(), eq(Duration.ofMinutes(2)));
        verify(hashOperations).put(SESSION_USERS_KEY, SESSION_ID, USER_ID);
        verify(hashOperations).put(eq(LAST_ACTIVE_KEY), eq(SESSION_ID), anyString());
        verify(setOperations).add(USER_SESSIONS_KEY, SESSION_ID);
    }

    @Test
    void heartbeatRefreshesTtlAndLastActiveTime() {
        when(redisTemplate.hasKey(HEARTBEAT_KEY)).thenReturn(true);
        when(hashOperations.get(LAST_ACTIVE_KEY, SESSION_ID)).thenReturn("1000");

        boolean result = onlineUserManager.heartbeat(httpSession, 2000L);

        assertTrue(result);
        verify(valueOperations).set(eq(HEARTBEAT_KEY), anyString(), eq(Duration.ofMinutes(2)));
        verify(hashOperations).put(LAST_ACTIVE_KEY, SESSION_ID, "2000");
    }

    @Test
    void heartbeatRejectsExpiredSessionAndCleansIndexes() {
        when(redisTemplate.hasKey(HEARTBEAT_KEY)).thenReturn(false);
        when(hashOperations.get(SESSION_USERS_KEY, SESSION_ID)).thenReturn(USER_ID);

        boolean result = onlineUserManager.heartbeat(httpSession, 2000L);

        assertFalse(result);
        verifySessionRemoved();
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void heartbeatRejectsOlderLastActiveTimeWithoutRemovingSession() {
        when(redisTemplate.hasKey(HEARTBEAT_KEY)).thenReturn(true);
        when(hashOperations.get(LAST_ACTIVE_KEY, SESSION_ID)).thenReturn("2000");

        boolean result = onlineUserManager.heartbeat(httpSession, 1000L);

        assertFalse(result);
        verify(redisTemplate, never()).delete(HEARTBEAT_KEY);
        verify(hashOperations, never()).put(eq(LAST_ACTIVE_KEY), any(), any());
    }

    @Test
    void logoutRemovesAllSessionData() {
        when(hashOperations.get(SESSION_USERS_KEY, SESSION_ID)).thenReturn(USER_ID);

        onlineUserManager.logout(httpSession);

        verifySessionRemoved();
    }

    @Test
    void statisticsIgnoreAndRemoveExpiredSessions() {
        String expiredSessionId = "session-2";
        when(hashOperations.keys(SESSION_USERS_KEY)).thenReturn(Set.of(SESSION_ID, expiredSessionId));
        when(redisTemplate.hasKey(HEARTBEAT_KEY)).thenReturn(true);
        when(redisTemplate.hasKey("online:heartbeat:" + expiredSessionId)).thenReturn(false);
        when(hashOperations.get(SESSION_USERS_KEY, SESSION_ID)).thenReturn(USER_ID);
        when(hashOperations.get(SESSION_USERS_KEY, expiredSessionId)).thenReturn(USER_ID);
        when(hashOperations.get(LAST_ACTIVE_KEY, SESSION_ID)).thenReturn("1000");

        assertEquals(1, onlineUserManager.getOnlineSessionCount());
        assertEquals(1, onlineUserManager.getOnlineUserCount());
        assertEquals(Map.of(USER_ID, 1L), onlineUserManager.getUserSessionCount());
        assertEquals(Map.of(USER_ID, 1000L), onlineUserManager.getUserLastActiveTime());
        verify(hashOperations, org.mockito.Mockito.times(4)).delete(SESSION_USERS_KEY, expiredSessionId);
    }

    @Test
    void rejectsLoginWhenOnlineUserLimitIsReached() {
        when(httpSession.getId()).thenReturn("new-session");
        when(httpSession.getAttribute("userId")).thenReturn(null);
        when(hashOperations.get(LIMITS_KEY, "maxOnlineUserCount")).thenReturn("1");
        when(hashOperations.keys(SESSION_USERS_KEY)).thenReturn(Set.of(SESSION_ID));
        when(redisTemplate.hasKey(HEARTBEAT_KEY)).thenReturn(true);
        when(hashOperations.get(SESSION_USERS_KEY, SESSION_ID)).thenReturn(USER_ID);

        int result = onlineUserManager.checkCanLogin("13", httpSession);

        assertEquals(OnlineUserManager.LOGIN_DENIED_BY_USER_COUNT, result);
    }

    @Test
    void allowsRepeatedLoginInSameSessionAtOnlineUserLimit() {
        when(hashOperations.get(LIMITS_KEY, "maxOnlineUserCount")).thenReturn("1");
        when(hashOperations.get(LIMITS_KEY, "maxSessionCountPerUser")).thenReturn("0");

        int result = onlineUserManager.checkCanLogin(USER_ID, httpSession);

        assertEquals(OnlineUserManager.LOGIN_ALLOWED, result);
    }

    @Test
    void rejectsLoginWhenUserSessionLimitIsReached() {
        when(httpSession.getId()).thenReturn("new-session");
        when(httpSession.getAttribute("userId")).thenReturn(null);
        when(hashOperations.get(LIMITS_KEY, "maxOnlineUserCount")).thenReturn("0");
        when(hashOperations.get(LIMITS_KEY, "maxSessionCountPerUser")).thenReturn("2");
        when(setOperations.members(USER_SESSIONS_KEY)).thenReturn(Set.of(SESSION_ID, "session-2"));
        when(redisTemplate.hasKey(HEARTBEAT_KEY)).thenReturn(true);
        when(redisTemplate.hasKey("online:heartbeat:session-2")).thenReturn(true);

        int result = onlineUserManager.checkCanLogin(USER_ID, httpSession);

        assertEquals(OnlineUserManager.LOGIN_DENIED_BY_SESSION_COUNT, result);
    }

    @Test
    void limitsUseDefaultsAndPersistChangesInRedis() {
        assertEquals(50, onlineUserManager.getMaxOnlineUserCount());
        assertEquals(2, onlineUserManager.getMaxSessionCountPerUser());

        onlineUserManager.setMaxOnlineUserCount(100);
        onlineUserManager.setMaxSessionCountPerUser(3);

        verify(hashOperations).put(LIMITS_KEY, "maxOnlineUserCount", "100");
        verify(hashOperations).put(LIMITS_KEY, "maxSessionCountPerUser", "3");
    }

    @Test
    void evictsHeartbeatExpiredAndIdleSessions() {
        String idleSessionId = "session-2";
        String activeSessionId = "session-3";
        when(hashOperations.keys(SESSION_USERS_KEY))
                .thenReturn(Set.of(SESSION_ID, idleSessionId, activeSessionId));
        when(redisTemplate.hasKey(HEARTBEAT_KEY)).thenReturn(false);
        when(redisTemplate.hasKey("online:heartbeat:" + idleSessionId)).thenReturn(true);
        when(redisTemplate.hasKey("online:heartbeat:" + activeSessionId)).thenReturn(true);
        when(hashOperations.get(eq(SESSION_USERS_KEY), any())).thenReturn(USER_ID);
        when(hashOperations.get(LAST_ACTIVE_KEY, idleSessionId)).thenReturn("0");
        when(hashOperations.get(LAST_ACTIVE_KEY, activeSessionId))
                .thenReturn(Long.toString(System.currentTimeMillis()));

        int deletedCount = onlineUserManager.evictStaleUsers();

        assertEquals(2, deletedCount);
        verify(redisTemplate).delete(HEARTBEAT_KEY);
        verify(redisTemplate).delete("online:heartbeat:" + idleSessionId);
        verify(redisTemplate, never()).delete("online:heartbeat:" + activeSessionId);
    }

    private void verifySessionRemoved() {
        verify(redisTemplate).delete(HEARTBEAT_KEY);
        verify(hashOperations).delete(SESSION_USERS_KEY, SESSION_ID);
        verify(hashOperations).delete(LAST_ACTIVE_KEY, SESSION_ID);
        verify(setOperations).remove(USER_SESSIONS_KEY, SESSION_ID);
    }
}
