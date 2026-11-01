package com.example.springdemo.service;

import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.dto.UserToLogin;
import com.example.springdemo.entity.UserPassword;
import com.example.springdemo.entity.UserPosition;
import com.example.springdemo.manager.LoginCounter;
import com.example.springdemo.manager.OnlineUserManager;
import com.example.springdemo.mapper.UserPasswordMapper;
import com.example.springdemo.util.BasicFunctions;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserLoginService {

    @Resource
    private MessageSource msgSource;

    @Resource
    private final UserPasswordMapper userPasswordMapper;

    @Resource
    private OnlineUserManager onlineUserManager;

    public UserLoginService(UserPasswordMapper userPasswordMapper) {
        this.userPasswordMapper = userPasswordMapper;
    }

    public UserPassword getUser(Long id) {
        return userPasswordMapper.selectById(id);
    }

    public boolean isAlreadyLoggedIn(@NonNull HttpSession httpSession){
        boolean result= BasicFunctions.isLoggedIn(httpSession);
        String userId=null;
        String log;
        if(httpSession.isNew()) {
            log= msgSource.getMessage("logMessage.newUserGoLogin",null, Locale.CHINA);
        }
        else {
            if (result) {
                userId=(String)httpSession.getAttribute("userId");
                log= msgSource.getMessage("logMessage.alreadyLoggedInUserGoLogin",null, Locale.CHINA);
            }
            else{
                log= msgSource.getMessage("logMessage.userGoLoginAgain",null, Locale.CHINA);
            }
        }
        if (userId==null) {
            BasicFunctions.printTimeHttpSessionAnd(httpSession, log);
        }
        else{
            BasicFunctions.printTimeHttpSessionAnd(httpSession, userId, log);
        }
        return result;
    }
    /**系统登录逻辑
     * @param httpSession 当前http会话
     * @param userToLogin 登录的用户（包括用户名、密码）
     * @return 登录结果，返回值（是否登录成功，
     *          用户身份(int，登录失败为-1)，
     *          失败原因（如果登录失败，否则为null））
     * @throws IllegalArgumentException 当数据库中用户身份不正确时
     *   */
    public RequestResult login(@NonNull HttpSession httpSession, @NonNull UserToLogin userToLogin){
        Long inputtedUserId = null;
        String user1Id=null,user2Id=null;// 将要写在日志中的userId
        String log = "";
        String reasonHint=null;
        boolean result=true;
        UserPassword userPassword;
        UserPosition userPosition = null;
        Locale frontLocale=Locale.CHINA;


        if(httpSession.getAttribute("userId")!=null){// 已经有用户登录了
            user1Id=userToLogin.getId();
            user2Id=(String)httpSession.getAttribute("userId");
            if(user1Id.equals(user2Id)){
                log= msgSource.getMessage("logMessage.loginAgain", null, Locale.CHINA);
            }
            else{
                result=false;
                log= msgSource.getMessage("logMessage.loginWithoutLogout", null, Locale.CHINA);
                reasonHint= msgSource.getMessage("responseMessage.otherUserAlreadyLoggedIn", null, frontLocale);
            }
        }
        if(result) {
            try {
                inputtedUserId = Long.parseLong(userToLogin.getId());
            } catch (NumberFormatException e) {
                result = false;
                log = msgSource.getMessage("logMessage.illegalUserId", new Object[]{userToLogin.getId()}, Locale.CHINA);
                reasonHint = msgSource.getMessage("responseMessage.illegalUserId", null, frontLocale);
            }
        }
        if(result) {
            userPassword=this.getUser(inputtedUserId);
            if (userPassword==null) {
                result=false;
                // 即使知道是用户不存在，也要返回“账号或密码错误”，防止暴力破解
                reasonHint= msgSource.getMessage("responseMessage.nameOrPasswordWrong", null, frontLocale);
                user1Id=userToLogin.getId();
                log= msgSource.getMessage("logMessage.userIdNotFound",null, Locale.CHINA);
            } else{
                if (BasicFunctions.safeEquals(userPassword.getPassword(), userToLogin.getPassword())) {
                    result=true;
                    String position=userPassword.getPosition();
                    if(position.equals(UserPosition.TEACHER.getString())){
                        LoginCounter.TEACHER.increment();
                        userPosition= UserPosition.TEACHER;
                    } else if (position.equals(UserPosition.STUDENT.getString())) {
                        LoginCounter.STUDENT.increment();
                        userPosition= UserPosition.STUDENT;
                    } else if (position.equals(UserPosition.ADMIN.getString())) {
                        LoginCounter.ADMIN.increment();
                        userPosition= UserPosition.ADMIN;
                    }
                    else{
                        throw new IllegalArgumentException("未知的用户类型: "+position);
                    }
                    log=msgSource.getMessage("logMessage.loginSucceed", null, Locale.CHINA);
                } else {
                    result=false;
                    user1Id=userToLogin.getId();
                    log= msgSource.getMessage("logMessage.wrongPassword", null, Locale.CHINA);
                    reasonHint= msgSource.getMessage("responseMessage.nameOrPasswordWrong", null, frontLocale);
                }
            }
        }

        if(result) {
            int canLoginCode = onlineUserManager.checkCanLogin(userToLogin.getId(), httpSession);
            if (canLoginCode == OnlineUserManager.LOGIN_DENIED_BY_USER_COUNT) {
                result=false;
                user1Id=userToLogin.getId();
                log= msgSource.getMessage("logMessage.loginFailUserCountLimit", null, Locale.CHINA);
                reasonHint= msgSource.getMessage("responseMessage.onlineUserCountLimitReached", null, frontLocale);
            }
            else if (canLoginCode == OnlineUserManager.LOGIN_DENIED_BY_SESSION_COUNT) {
                result=false;
                user1Id=userToLogin.getId();
                log= msgSource.getMessage("logMessage.loginFailSessionCountLimit", null, Locale.CHINA);
                reasonHint= msgSource.getMessage("responseMessage.onlineUserSessionCountLimitReached", null, frontLocale);
            }
        }
        if(user1Id!=null && user2Id!=null){
            BasicFunctions.printTimeHttpSessionAnd(httpSession, user1Id, user2Id,log);
        }
        else if(user1Id!=null){
            BasicFunctions.printTimeHttpSessionAnd(httpSession, user1Id,log);
        }
        else{
            BasicFunctions.printTimeHttpSessionAnd(httpSession,log);
        }
        if(result) {
            httpSession.setAttribute("userId",userToLogin.getId());
            httpSession.setAttribute("userPosition", userPosition);
            onlineUserManager.login(httpSession);
            return new RequestResult(true, userPosition.getCode(), null);
        }
        else{
            return new RequestResult(false, -1, reasonHint);
        }

    }
}
