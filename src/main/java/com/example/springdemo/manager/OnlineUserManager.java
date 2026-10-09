package com.example.springdemo.manager;


import com.example.springdemo.util.BasicFunctions;
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

    /** key: sessionId, value: 最近心跳时间*/
    private final Map<HttpSession, Long> onlineUsers = new ConcurrentHashMap<>();

    /** key: sessionId, value: 最近操作时间*/
    private final Map<HttpSession, Long> onlineUsersLastActive = new ConcurrentHashMap<>();

    /** 心跳超时阈值：超过 2 分钟无心跳视为离线 */
    private static final long HEART_BEAT_TIMEOUT = 2 * 60 * 1000L;

    /** 操作超时阈值：超过 6 分钟无操作视为离线 */
    private static final long LAST_ACTIVE_TIMEOUT = 6 * 60 * 1000L;

    public void login(HttpSession httpSession) {
        onlineUsers.put(httpSession, System.currentTimeMillis());
        onlineUsersLastActive.put(httpSession, System.currentTimeMillis());
    }

    public boolean heartbeat(@NonNull HttpSession httpSession, @NonNull Long lastActiveTime) {
        Long storedLastActive = onlineUsersLastActive.get(httpSession);
//        System.out.println(lastActiveTime+"\t"+storedLastActive);
        if(lastActiveTime<storedLastActive){
            return false;
        }

        onlineUsers.computeIfPresent(httpSession,
                (k, v) -> System.currentTimeMillis());
        onlineUsersLastActive.replace(httpSession, lastActiveTime);
        return true;
    }

    public void logout(HttpSession httpSession) {// 用户手动注销或超时未操作而自动注销
        onlineUsers.remove(httpSession);
        onlineUsersLastActive.remove(httpSession);
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

    public void logoutDueToNoActionByBackend(@NonNull HttpSession httpSession){
        Object userId=httpSession.getAttribute("userId");
        if(userId==null){
            return;
        }
        httpSession.setAttribute("userId",null);
        httpSession.setAttribute("userPosition",null);
        BasicFunctions.printTimeHttpSessionAnd(httpSession, (String) userId,
                msgSource.getMessage("logMessage.loggedOutDueToNoAction_backend", null, Locale.CHINA));

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
                .filter(e -> now - e.getValue() > HEART_BEAT_TIMEOUT)
                .forEach(e -> {
                    logoutDueToTimeOut(e.getKey());
                    onlineUsersLastActive.remove(e.getKey());
                });
        onlineUsersLastActive.entrySet()
                .stream()
                .filter(e -> now-e.getValue()>LAST_ACTIVE_TIMEOUT)
                .forEach(e -> {
                    logoutDueToTimeOut(e.getKey());
                    onlineUsers.remove(e.getKey());
                });
        afterCount= getOnlineSessionCount();
        return beforeCount-afterCount;
    }
}
