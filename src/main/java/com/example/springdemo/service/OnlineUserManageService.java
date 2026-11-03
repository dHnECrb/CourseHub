package com.example.springdemo.service;

import com.example.springdemo.dto.OnlineCountsView;
import com.example.springdemo.dto.OnlineLimitsView;
import com.example.springdemo.dto.OnlineUserView;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.entity.User;
import com.example.springdemo.entity.UserPosition;
import com.example.springdemo.manager.LoginCounter;
import com.example.springdemo.manager.OnlineUserManager;
import com.example.springdemo.mapper.UserMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OnlineUserManageService {

    @Resource
    private MessageSource msgSource;

    @Resource
    private OnlineUserManager onlineUserManager;

    @Resource
    private LoginCounter loginCounter;

    @Resource
    private UserMapper userMapper;

    private boolean isAdmin(@NonNull HttpSession httpSession){
        if(httpSession.getAttribute("userId")==null){
            return false;
        }
        Object position=httpSession.getAttribute("userPosition");
        return position instanceof UserPosition && position == UserPosition.ADMIN;
    }

    /**管理员进入在线用户管理页面
     * @param httpSession 当前http会话
     * @return 是否允许进入（未登录 401，非管理员 403）*/
    public RequestResult index(@NonNull HttpSession httpSession){
        Locale locale=Locale.CHINA;
        if(httpSession.getAttribute("userId")==null) {
            return new RequestResult(false, 401,
                    msgSource.getMessage("responseMessage.didNotLogin", null, locale));
        }
        if(!isAdmin(httpSession)){
            return new RequestResult(false, 403,
                    msgSource.getMessage("responseMessage.accessDenied", null, locale));
        }
        return new RequestResult(true, 200, null);
    }

    /**获取在线用户数量、在线会话数量与各角色累计登录次数，无权限时返回 null*/
    public OnlineCountsView getCounts(@NonNull HttpSession httpSession){
        if(!isAdmin(httpSession)){
            return null;
        }
        return new OnlineCountsView(onlineUserManager.getOnlineUserCount(),
                onlineUserManager.getOnlineSessionCount(),
                loginCounter.getCount(UserPosition.ADMIN),
                loginCounter.getCount(UserPosition.STUDENT),
                loginCounter.getCount(UserPosition.TEACHER));
    }

    /**获取当前的在线用户数量上限与单用户会话数量上限，无权限时返回 null*/
    public OnlineLimitsView getLimits(@NonNull HttpSession httpSession){
        if(!isAdmin(httpSession)){
            return null;
        }
        return new OnlineLimitsView(onlineUserManager.getMaxOnlineUserCount(),
                onlineUserManager.getMaxSessionCountPerUser());
    }

    /**设置在线用户数量上限与单用户会话数量上限（0 表示不限制），无权限或参数非法时返回失败*/
    public RequestResult setLimits(@NonNull HttpSession httpSession,
                                   int maxOnlineUserCount, int maxSessionCountPerUser){
        Locale locale=Locale.CHINA;
        if(!isAdmin(httpSession)){
            return new RequestResult(false, 403,
                    msgSource.getMessage("responseMessage.accessDenied", null, locale));
        }
        if(maxOnlineUserCount<0 || maxSessionCountPerUser<0){
            return new RequestResult(false, -1,
                    msgSource.getMessage("responseMessage.illegalOnlineLimits", null, locale));
        }
        onlineUserManager.setMaxOnlineUserCount(maxOnlineUserCount);
        onlineUserManager.setMaxSessionCountPerUser(maxSessionCountPerUser);
        return new RequestResult(true, 200, null);
    }

    /**获取每个在线用户的账号、姓名、会话数量、最近活跃时间，按账号升序；无权限时返回空列表*/
    public List<OnlineUserView> getUsers(@NonNull HttpSession httpSession){
        if(!isAdmin(httpSession)){
            return List.of();
        }
        Map<String, Long> sessionCountByUser = onlineUserManager.getUserSessionCount();
        Map<String, Long> lastActiveTimeByUser = onlineUserManager.getUserLastActiveTime();

        // 账号即 user 表的编号，批量查出姓名
        List<Long> userIds = sessionCountByUser.keySet().stream()
                .map(account -> {
                    try {
                        return Long.parseLong(account);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        Map<Long, String> nameById = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getName));

        return sessionCountByUser.entrySet().stream()
                .map(entry -> {
                    String account = entry.getKey();
                    Long userId;
                    try {
                        userId = Long.parseLong(account);
                    } catch (NumberFormatException e) {
                        userId = null;
                    }
                    return new OnlineUserView(account,
                            userId == null ? null : nameById.get(userId),
                            entry.getValue(),
                            lastActiveTimeByUser.getOrDefault(account, 0L));
                })
                .sorted(Comparator.comparing(OnlineUserView::getAccount))
                .toList();
    }
}
