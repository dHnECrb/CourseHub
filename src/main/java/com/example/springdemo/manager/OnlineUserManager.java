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

    /** 在线用户数量上限，默认 50；0 表示不限制 */
    private volatile int maxOnlineUserCount = 50;

    /** 单个用户的会话数量上限，默认 2；0 表示不限制 */
    private volatile int maxSessionCountPerUser = 2;

    /** 检查登录是否被数量限制所拒绝：可以登录 */
    public static final int LOGIN_ALLOWED = 0;
    /** 检查登录是否被数量限制所拒绝：在线用户数量已达上限 */
    public static final int LOGIN_DENIED_BY_USER_COUNT = 1;
    /** 检查登录是否被数量限制所拒绝：该用户的会话数量已达上限 */
    public static final int LOGIN_DENIED_BY_SESSION_COUNT = 2;

    public int getMaxOnlineUserCount() {
        return maxOnlineUserCount;
    }

    public void setMaxOnlineUserCount(int maxOnlineUserCount) {
        this.maxOnlineUserCount = maxOnlineUserCount;
    }

    public int getMaxSessionCountPerUser() {
        return maxSessionCountPerUser;
    }

    public void setMaxSessionCountPerUser(int maxSessionCountPerUser) {
        this.maxSessionCountPerUser = maxSessionCountPerUser;
    }

    /** 检查指定用户能否在当前会话登录（受在线用户数量与单用户会话数量限制）
     * @param userId 将要登录的账号
     * @param httpSession 当前http会话
     * @return LOGIN_ALLOWED / LOGIN_DENIED_BY_USER_COUNT / LOGIN_DENIED_BY_SESSION_COUNT */
    public int checkCanLogin(@NonNull String userId, @NonNull HttpSession httpSession) {
        // 当前会话已登录同一账号时属于重复登录，不会新增用户与会话，不受限制
        boolean alreadyInThisSession = userId.equals(httpSession.getAttribute("userId"));
        if (maxOnlineUserCount > 0 && !alreadyInThisSession
                && getOnlineUserCount() >= maxOnlineUserCount) {
            return LOGIN_DENIED_BY_USER_COUNT;
        }
        if (maxSessionCountPerUser > 0) {
            long otherSessionCount = onlineUsers.keySet().stream()
                    .filter(session -> session != httpSession)
                    .map(session -> (String) session.getAttribute("userId"))
                    .filter(userId::equals)
                    .count();
            if (otherSessionCount >= maxSessionCountPerUser) {
                return LOGIN_DENIED_BY_SESSION_COUNT;
            }
        }
        return LOGIN_ALLOWED;
    }

    /**获取每个在线用户的最近活跃时间（其所有会话最近操作时间的最晚值）
     * @return key: 账号，value: 最近活跃时间（毫秒时间戳）*/
    public Map<String, Long> getUserLastActiveTime(){
        Map<String, Long> lastActiveTimeByUser = new java.util.HashMap<>();
        onlineUsersLastActive.forEach((session, lastActiveTime) -> {
            String userId = (String) session.getAttribute("userId");
            if (userId != null) {
                lastActiveTimeByUser.merge(userId, lastActiveTime, Math::max);
            }
        });
        return lastActiveTimeByUser;
    }

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

    private void logoutAuto(@NonNull HttpSession httpSession, String message){
        Object userId;
        try {
            userId = httpSession.getAttribute("userId");
        } catch (IllegalStateException e) {
            BasicFunctions.printTimeHttpSessionAnd(httpSession,
                    msgSource.getMessage("logMessage.loggedOutInvalidatedSession", null, Locale.CHINA));
            return;
        }
        if(userId==null){
            return;
        }
        httpSession.setAttribute("userId",null);
        httpSession.setAttribute("userPosition",null);
        BasicFunctions.printTimeHttpSessionAnd(httpSession, (String) userId, message);

    }

    public void logoutDueToTimeOut(@NonNull HttpSession httpSession){
        logoutAuto(httpSession, msgSource.getMessage("logMessage.loggedOutDueToHeartBeatTimeOut", null, Locale.CHINA));
    }

    public void logoutDueToNoActionByBackend(@NonNull HttpSession httpSession){
        logoutAuto(httpSession, msgSource.getMessage("logMessage.loggedOutDueToNoAction_backend", null, Locale.CHINA));

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
                    onlineUsers.remove(e.getKey());
                    onlineUsersLastActive.remove(e.getKey());
                });
        onlineUsersLastActive.entrySet()
                .stream()
                .filter(e -> now-e.getValue()>LAST_ACTIVE_TIMEOUT)
                .forEach(e -> {
                    logoutDueToNoActionByBackend(e.getKey());
                    onlineUsers.remove(e.getKey());
                    onlineUsersLastActive.remove(e.getKey());
                });
        afterCount= getOnlineSessionCount();
        return beforeCount-afterCount;
    }
}
