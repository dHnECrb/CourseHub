package com.example.springdemo.manager;

import com.example.springdemo.util.BasicFunctions;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OnlineUserManager {
    @Resource
    private MessageSource msgSource;

    private static final String HEARTBEAT_KEY_PREFIX = "online:heartbeat:";
    private static final String SESSION_USERS_KEY = "online:sessionUsers";
    private static final String LAST_ACTIVE_KEY = "online:lastActive";
    private static final String USER_SESSIONS_KEY_PREFIX = "online:user:";
    private static final String LIMITS_KEY = "online:limits";
    private static final String MAX_ONLINE_USER_COUNT_FIELD = "maxOnlineUserCount";
    private static final String MAX_SESSION_COUNT_PER_USER_FIELD = "maxSessionCountPerUser";

    /** 心跳超时阈值：超过 2 分钟无心跳视为离线 */
    private static final Duration HEART_BEAT_TIMEOUT = Duration.ofMinutes(2);

    /** 操作超时阈值：超过 6 分钟无操作视为离线 */
    private static final long LAST_ACTIVE_TIMEOUT = 6 * 60 * 1000L;

    private static final int DEFAULT_MAX_ONLINE_USER_COUNT = 50;
    private static final int DEFAULT_MAX_SESSION_COUNT_PER_USER = 2;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    /** 检查登录是否被数量限制所拒绝：可以登录 */
    public static final int LOGIN_ALLOWED = 0;
    /** 检查登录是否被数量限制所拒绝：在线用户数量已达上限 */
    public static final int LOGIN_DENIED_BY_USER_COUNT = 1;
    /** 检查登录是否被数量限制所拒绝：该用户的会话数量已达上限 */
    public static final int LOGIN_DENIED_BY_SESSION_COUNT = 2;

    public int getMaxOnlineUserCount() {
        return getLimit(MAX_ONLINE_USER_COUNT_FIELD, DEFAULT_MAX_ONLINE_USER_COUNT);
    }

    public void setMaxOnlineUserCount(int maxOnlineUserCount) {
        redisTemplate.opsForHash().put(LIMITS_KEY, MAX_ONLINE_USER_COUNT_FIELD,
                Integer.toString(maxOnlineUserCount));
    }

    public int getMaxSessionCountPerUser() {
        return getLimit(MAX_SESSION_COUNT_PER_USER_FIELD, DEFAULT_MAX_SESSION_COUNT_PER_USER);
    }

    public void setMaxSessionCountPerUser(int maxSessionCountPerUser) {
        redisTemplate.opsForHash().put(LIMITS_KEY, MAX_SESSION_COUNT_PER_USER_FIELD,
                Integer.toString(maxSessionCountPerUser));
    }

    private int getLimit(String field, int defaultValue) {
        Object value = redisTemplate.opsForHash().get(LIMITS_KEY, field);
        return value == null ? defaultValue : Integer.parseInt(value.toString());
    }

    /** 检查指定用户能否在当前会话登录（受在线用户数量与单用户会话数量限制）
     * @param userId 将要登录的账号
     * @param httpSession 当前http会话
     * @return LOGIN_ALLOWED / LOGIN_DENIED_BY_USER_COUNT / LOGIN_DENIED_BY_SESSION_COUNT */
    public int checkCanLogin(@NonNull String userId, @NonNull HttpSession httpSession) {
        String sessionId = httpSession.getId();
        boolean alreadyInThisSession = userId.equals(httpSession.getAttribute("userId"));
        int maxOnlineUserCount = getMaxOnlineUserCount();
        if (maxOnlineUserCount > 0 && !alreadyInThisSession
                && getOnlineUserCount() >= maxOnlineUserCount) {
            return LOGIN_DENIED_BY_USER_COUNT;
        }
        int maxSessionCountPerUser = getMaxSessionCountPerUser();
        if (maxSessionCountPerUser > 0) {
            long otherSessionCount = getActiveUserSessionIds(userId).stream()
                    .filter(id -> !id.equals(sessionId))
                    .count();
            if (otherSessionCount >= maxSessionCountPerUser) {
                return LOGIN_DENIED_BY_SESSION_COUNT;
            }
        }
        return LOGIN_ALLOWED;
    }

    /** 获取每个在线用户的最近活跃时间（其所有会话最近操作时间的最晚值）
     * @return key: 账号，value: 最近活跃时间（毫秒时间戳） */
    public Map<String, Long> getUserLastActiveTime() {
        Map<String, Long> lastActiveTimeByUser = new HashMap<>();
        for (String sessionId : getActiveSessionIds()) {
            String userId = getSessionUserId(sessionId);
            Long lastActiveTime = getLastActiveTime(sessionId);
            if (userId != null && lastActiveTime != null) {
                lastActiveTimeByUser.merge(userId, lastActiveTime, Math::max);
            }
        }
        return lastActiveTimeByUser;
    }

    public void login(@NonNull HttpSession httpSession) {
        String sessionId = httpSession.getId();
        String userId = (String) httpSession.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("登录会话缺少用户账号");
        }
        long now = System.currentTimeMillis();
        refreshHeartbeat(sessionId, now);
        redisTemplate.opsForHash().put(SESSION_USERS_KEY, sessionId, userId);
        redisTemplate.opsForHash().put(LAST_ACTIVE_KEY, sessionId, Long.toString(now));
        redisTemplate.opsForSet().add(userSessionsKey(userId), sessionId);
    }

    public boolean isOnline(@NonNull HttpSession httpSession) {
        return hasHeartbeat(httpSession.getId());
    }

    public boolean heartbeat(@NonNull HttpSession httpSession, @NonNull Long lastActiveTime) {
        String sessionId = httpSession.getId();
        if (!hasHeartbeat(sessionId)) {
            removeSession(sessionId);
            return false;
        }
        Long storedLastActive = getLastActiveTime(sessionId);
        if (storedLastActive == null) {
            removeSession(sessionId);
            return false;
        }
        if (lastActiveTime < storedLastActive) {
            return false;
        }

        long now = System.currentTimeMillis();
        refreshHeartbeat(sessionId, now);
        redisTemplate.opsForHash().put(LAST_ACTIVE_KEY, sessionId, lastActiveTime.toString());
        return true;
    }

    /** 用户手动注销或超时未操作而自动注销 */
    public void logout(@NonNull HttpSession httpSession) {
        removeSession(httpSession.getId());
    }

    /** 获取已登录会话数量（注意同一个用户可以在多个会话中登录） */
    public int getOnlineSessionCount() {
        return getActiveSessionIds().size();
    }

    /** 获取已登录用户数量，同一用户在不同会话中登录，只算1个 */
    public int getOnlineUserCount() {
        return getActiveSessionIds().stream()
                .map(this::getSessionUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .size();
    }

    /** 获取已登录用户的会话数量 */
    public Map<String, Long> getUserSessionCount() {
        return getActiveSessionIds().stream()
                .map(this::getSessionUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
    }

    /** 由定时任务调用，剔除僵尸用户会话 */
    public int evictStaleUsers() {
        int deletedCount = 0;
        long now = System.currentTimeMillis();
        for (String sessionId : getAllSessionIds()) {
            String userId = getSessionUserId(sessionId);
            if (!hasHeartbeat(sessionId)) {
                if (userId != null) {
                    BasicFunctions.printTimeHttpSessionIdAnd(sessionId, userId,
                            msgSource.getMessage("logMessage.loggedOutDueToHeartBeatTimeOut", null, Locale.CHINA));
                }
                removeSession(sessionId);
                deletedCount++;
                continue;
            }
            Long lastActiveTime = getLastActiveTime(sessionId);
            if (lastActiveTime == null || now - lastActiveTime > LAST_ACTIVE_TIMEOUT) {
                if (userId != null) {
                    BasicFunctions.printTimeHttpSessionIdAnd(sessionId, userId,
                            msgSource.getMessage("logMessage.loggedOutDueToNoAction_backend", null, Locale.CHINA));
                }
                removeSession(sessionId);
                deletedCount++;
            }
        }
        return deletedCount;
    }

    private void refreshHeartbeat(String sessionId, long heartbeatTime) {
        redisTemplate.opsForValue().set(heartbeatKey(sessionId), Long.toString(heartbeatTime),
                HEART_BEAT_TIMEOUT);
    }

    private boolean hasHeartbeat(String sessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(heartbeatKey(sessionId)));
    }

    private String getSessionUserId(String sessionId) {
        Object userId = redisTemplate.opsForHash().get(SESSION_USERS_KEY, sessionId);
        return userId == null ? null : userId.toString();
    }

    private Long getLastActiveTime(String sessionId) {
        Object lastActiveTime = redisTemplate.opsForHash().get(LAST_ACTIVE_KEY, sessionId);
        return lastActiveTime == null ? null : Long.parseLong(lastActiveTime.toString());
    }

    private Set<String> getAllSessionIds() {
        Set<Object> sessionIds = redisTemplate.opsForHash().keys(SESSION_USERS_KEY);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Set.of();
        }
        return sessionIds.stream().map(Object::toString).collect(Collectors.toSet());
    }

    private Set<String> getActiveSessionIds() {
        Set<String> activeSessionIds = new HashSet<>();
        for (String sessionId : getAllSessionIds()) {
            if (hasHeartbeat(sessionId)) {
                activeSessionIds.add(sessionId);
            }
            else {
                removeSession(sessionId);
            }
        }
        return activeSessionIds;
    }

    private Set<String> getActiveUserSessionIds(String userId) {
        Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey(userId));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Set.of();
        }
        Set<String> activeSessionIds = new HashSet<>();
        for (String sessionId : sessionIds) {
            if (hasHeartbeat(sessionId)) {
                activeSessionIds.add(sessionId);
            }
            else {
                removeSession(sessionId);
            }
        }
        return activeSessionIds;
    }

    private void removeSession(String sessionId) {
        String userId = getSessionUserId(sessionId);
        redisTemplate.delete(heartbeatKey(sessionId));
        redisTemplate.opsForHash().delete(SESSION_USERS_KEY, sessionId);
        redisTemplate.opsForHash().delete(LAST_ACTIVE_KEY, sessionId);
        if (userId != null) {
            redisTemplate.opsForSet().remove(userSessionsKey(userId), sessionId);
        }
    }

    private static String heartbeatKey(String sessionId) {
        return HEARTBEAT_KEY_PREFIX + sessionId;
    }

    private static String userSessionsKey(String userId) {
        return USER_SESSIONS_KEY_PREFIX + userId;
    }
}
