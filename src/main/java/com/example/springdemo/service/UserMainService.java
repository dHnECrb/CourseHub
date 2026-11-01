package com.example.springdemo.service;

import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.manager.OnlineUserManager;
//import com.example.springdemo.mapper.UserMapper;
//import com.example.springdemo.mapper.UserPasswordMapper;
import com.example.springdemo.util.BasicFunctions;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

@Service
public class UserMainService {
//    @Resource
//    private UserMapper userMapper;
//
//    @Resource
//    private UserPasswordMapper userPasswordMapper;

    @Resource
    private OnlineUserManager onlineUserManager;

    @Resource
    private MessageSource msgSource;

    public boolean canGoUserMain(@NonNull HttpSession httpSession){
        return this.canGoUserMain(httpSession, true);
    }

    public boolean canGoUserMain(@NonNull HttpSession httpSession, boolean isPrintLog){
        if (BasicFunctions.isLoggedIn(httpSession)){
            if(isPrintLog) {
                BasicFunctions.printTimeHttpSessionAnd(httpSession,
                        (String) httpSession.getAttribute("userId"),
                        "用户进入主界面");
            }
            return true;
        }
        else{
            if(isPrintLog) {
                BasicFunctions.printTimeHttpSessionAnd(httpSession, "有未登录的用户尝试进入主界面");
            }
            return false;
        }
    }

    public RequestResult heartbeat(@NonNull HttpSession httpSession, @NonNull Long lastActiveTime){
        boolean isSucceed;
        if(BasicFunctions.isLoggedIn(httpSession)){
            isSucceed=onlineUserManager.heartbeat(httpSession, lastActiveTime);
//            BasicFunctions.printTimeHttpSessionAnd(httpSession, (String)httpSession.getAttribute("userId"),
//                    "心跳\t"+ Instant.ofEpochMilli(lastActiveTime)
//                            .atZone(ZoneId.systemDefault())
//                            .toLocalDateTime());
            return new RequestResult(isSucceed, 200, null);
        }
        else{
            BasicFunctions.printTimeHttpSessionAnd(httpSession, "未登录用户心跳");
            return new RequestResult(false, 401, null);
        }
    }


    public void logoutManually(@NonNull HttpSession httpSession){
        Object userId=httpSession.getAttribute("userId");
        httpSession.setAttribute("userId",null);
        httpSession.setAttribute("userPosition",null);
        if(userId==null){
            BasicFunctions.printTimeHttpSessionAnd(httpSession,
                    msgSource.getMessage("logMessage.tryLogOutWithoutLogIn", null, Locale.CHINA));
        }
        else {
            onlineUserManager.logout(httpSession);
            BasicFunctions.printTimeHttpSessionAnd(httpSession, (String) userId,
                    msgSource.getMessage("logMessage.loggedOut", null, Locale.CHINA));
        }
    }

    public void logoutDueToNoAction(@NonNull HttpSession httpSession){
        Object userId=httpSession.getAttribute("userId");
        httpSession.setAttribute("userId",null);
        httpSession.setAttribute("userPosition",null);
        if(userId==null){

        }
        else {
            onlineUserManager.logout(httpSession);
            BasicFunctions.printTimeHttpSessionAnd(httpSession, (String) userId,
                    msgSource.getMessage("logMessage.loggedOutDueToNoAction", null, Locale.CHINA));
        }
    }

}
