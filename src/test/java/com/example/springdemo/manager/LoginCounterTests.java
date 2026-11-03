package com.example.springdemo.manager;

import com.example.springdemo.entity.UserPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginCounterTests {
    private static final String LOGIN_COUNTER_KEY = "login:counter";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private LoginCounter loginCounter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void incrementsRoleCounterAtomically() {
        loginCounter.increment(UserPosition.STUDENT);

        verify(hashOperations).increment(LOGIN_COUNTER_KEY, "student", 1);
    }

    @Test
    void missingCounterIsZero() {
        assertEquals(0, loginCounter.getCount(UserPosition.ADMIN));
    }

    @Test
    void totalCountSumsAllRoles() {
        when(hashOperations.get(LOGIN_COUNTER_KEY, "admin")).thenReturn("2");
        when(hashOperations.get(LOGIN_COUNTER_KEY, "student")).thenReturn("3");
        when(hashOperations.get(LOGIN_COUNTER_KEY, "teacher")).thenReturn("4");

        assertEquals(9, loginCounter.getTotalCount());
    }
}
