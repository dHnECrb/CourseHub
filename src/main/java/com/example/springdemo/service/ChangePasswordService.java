package com.example.springdemo.service;

import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.entity.UserPassword;
import com.example.springdemo.mapper.UserPasswordMapper;
import com.example.springdemo.util.BasicFunctions;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class ChangePasswordService {

    private static final String SITE = "/change_password";

    /**新密码的最小与最大长度*/
    public static final int MIN_PASSWORD_LENGTH = 9;
    public static final int MAX_PASSWORD_LENGTH = 25;

    @Resource
    private UserTaskService userTaskService;

    @Resource
    private UserPasswordMapper userPasswordMapper;

    @Resource
    private MessageSource msgSource;

    /**用户进入修改密码页面，入口由 task 表控制（模仿选课功能）
     * @param httpSession 当前http会话
     * @return 是否允许进入（未登录 401，无权限/时间不合适/任务不存在 403/404，附带提示信息）*/
    public RequestResult index(@NonNull HttpSession httpSession) {
        Locale locale = Locale.CHINA;
        String taskName = "修改密码";
        return userTaskService.goTask(httpSession, taskName, SITE, locale);
    }

    /**修改当前登录用户的密码
     * @param httpSession 当前http会话
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     * @return 修改结果，失败时附带提示信息*/
    public RequestResult changePassword(@NonNull HttpSession httpSession,
                                        String oldPassword, String newPassword) {
        Locale locale = Locale.CHINA;
        String userId = (String) httpSession.getAttribute("userId");

        if (!userTaskService.tryGoTask(httpSession, SITE).isSucceed()) {
            return new RequestResult(false, 403,
                    msgSource.getMessage("responseMessage.accessDenied", null, locale));
        }

        UserPassword userPassword = userPasswordMapper.selectById(Long.parseLong(userId));
        if (userPassword == null) {
            return new RequestResult(false, 404,
                    msgSource.getMessage("responseMessage.criticalError", null, locale));
        }

        String oldPasswordHash = BasicFunctions.hashPassword(
                oldPassword == null ? "" : oldPassword, userPassword.getSalt());
        if (!BasicFunctions.safeEquals(userPassword.getPassword(), oldPasswordHash)) {
            BasicFunctions.printTimeHttpSessionAnd(httpSession, userId,
                    msgSource.getMessage("logMessage.changePasswordFailWrongOld", null, Locale.CHINA));
            return new RequestResult(false, -1,
                    msgSource.getMessage("responseMessage.oldPasswordWrong", null, locale));
        }

        int length = newPassword == null ? 0 : newPassword.length();
        if (length < MIN_PASSWORD_LENGTH || length > MAX_PASSWORD_LENGTH) {
            BasicFunctions.printTimeHttpSessionAnd(httpSession, userId,
                    msgSource.getMessage("logMessage.changePasswordFailIllegalNew", null, Locale.CHINA));
            return new RequestResult(false, -1,
                    msgSource.getMessage("responseMessage.newPasswordLengthIllegal", null, locale));
        }

        // 重新生成盐并计算新哈希，同时更新 password 与 salt
        String newSalt = BasicFunctions.generateSalt();
        String newHash = BasicFunctions.hashPassword(newPassword, newSalt);
        userPasswordMapper.updateById(
                new UserPassword(userPassword.getId(), newHash, newSalt, userPassword.getPosition()));

        BasicFunctions.printTimeHttpSessionAnd(httpSession, userId,
                msgSource.getMessage("logMessage.changePasswordSucceed", null, Locale.CHINA));
        return new RequestResult(true, 200,
                msgSource.getMessage("responseMessage.passwordChanged", null, locale));
    }
}
