package com.example.springdemo.manager;

import com.example.springdemo.entity.UserPosition;
import jakarta.annotation.Resource;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 记录系统登录过的次数，包括一个用户前后多次登录 */
@Component
public class LoginCounter {
    private static final String LOGIN_COUNTER_KEY = "login:counter";

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    public void increment(@NonNull UserPosition userPosition) {
        redisTemplate.opsForHash().increment(LOGIN_COUNTER_KEY, userPosition.getString(), 1);
    }

    public long getCount(@NonNull UserPosition userPosition) {
        Object count = redisTemplate.opsForHash().get(LOGIN_COUNTER_KEY, userPosition.getString());
        return count == null ? 0 : Long.parseLong(count.toString());
    }

    public long getTotalCount() {
        long totalCount = 0;
        for (UserPosition userPosition : UserPosition.values()) {
            totalCount += getCount(userPosition);
        }
        return totalCount;
    }
}
