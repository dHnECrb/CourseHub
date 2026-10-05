package com.example.springdemo.manager;


import com.example.springdemo.service.BasicFunctions;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Component
public class OnlineUserManager {
    @Resource
    private MessageSource msgSource;

    /** key: sessionId, value: 最近活跃时间*/
    private final Map<HttpSession, Long> onlineUsers = new ConcurrentHashMap<>();

    /** 心跳超时阈值：超过 2 分钟无心跳视为离线 */
    private static final long TIMEOUT = 2 * 60 * 1000L;

    public void login(HttpSession httpSession) {
        onlineUsers.put(httpSession, System.currentTimeMillis());
    }

    public void heartbeat(HttpSession httpSession) {
        onlineUsers.computeIfPresent(httpSession,
                (k, v) -> System.currentTimeMillis());
    }

    public void logout(HttpSession httpSession) {// 用户手动注销或超时未操作而自动注销
        onlineUsers.remove(httpSession);
    }

    public void logoutDueToTimeOut(@NonNull HttpSession httpSession){
        Object userId=httpSession.getAttribute("userId");
        if(userId==null){
            return;
        }
        httpSession.setAttribute("userId",null);
        httpSession.setAttribute("userPosition",null);
        BasicFunctions.printTimeHttpSessionAnd(httpSession, (String) userId,
                msgSource.getMessage("logMessage.loggedOutDueToTimeOut", null, Locale.CHINA));
    }

    /**获取已登录会话数量（注意同一个用户可以在多个会话中登录）*/
    public int getOnlineSessionCount() {
        return onlineUsers.size();
    }

    /**获取已登录用户数量，同一用户在不同会话中登录，只算1个*/
    public int getOnlineUserCount(){
        return onlineUsers.keySet().stream()
                .map(session -> (String) session.getAttribute("userId"))
                .collect(Collectors.toSet())
                .size();
    }

    /**获取已登录用户的会话数量*/
    public Map<String, Long> getUserSessionCount(){
        return onlineUsers.keySet().stream()
                .map(session -> (String) session.getAttribute("userId"))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
    }

    /** 由定时任务调用，剔除僵尸用户会话 */
    public int evictStaleUsers() {
        int beforeCount= getOnlineSessionCount();
        int afterCount;
        long now = System.currentTimeMillis();
//        onlineUsers.entrySet().removeIf(e -> now - e.getValue() > TIMEOUT);
        onlineUsers.entrySet()
                .stream()
                .filter(e -> now - e.getValue() > TIMEOUT)
                .forEach(e -> {
                    logoutDueToTimeOut(e.getKey());
                });
        afterCount= getOnlineSessionCount();
        return beforeCount-afterCount;
    }
}
