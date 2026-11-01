package com.example.springdemo.service;

import com.example.springdemo.dto.CreatedUserView;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.entity.User;
import com.example.springdemo.entity.UserPassword;
import com.example.springdemo.entity.UserPosition;
import com.example.springdemo.mapper.UserMapper;
import com.example.springdemo.mapper.UserPasswordMapper;
import com.example.springdemo.util.BasicFunctions;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UserManageService {

    @Resource
    private MessageSource msgSource;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserPasswordMapper userPasswordMapper;

    private boolean isAdmin(@NonNull HttpSession httpSession) {
        if (httpSession.getAttribute("userId") == null) {
            return false;
        }
        Object position = httpSession.getAttribute("userPosition");
        return position instanceof UserPosition && position == UserPosition.ADMIN;
    }

    /**管理员进入用户管理页面
     * @param httpSession 当前http会话
     * @return 是否允许进入（未登录 401，非管理员 403）*/
    public RequestResult index(@NonNull HttpSession httpSession) {
        Locale locale = Locale.CHINA;
        if (httpSession.getAttribute("userId") == null) {
            return new RequestResult(false, 401,
                    msgSource.getMessage("responseMessage.didNotLogin", null, locale));
        }
        if (!isAdmin(httpSession)) {
            return new RequestResult(false, 403,
                    msgSource.getMessage("responseMessage.accessDenied", null, locale));
        }
        return new RequestResult(true, 200, null);
    }

    /**批量创建用户：id 为空、已存在于数据库或在本批次内重复的都不插入，其余正常插入。
     * 每个用户的盐由后端随机生成；初始密码留空则与 id 相同；其余字段留空存 null。
     * @param httpSession 当前http会话
     * @param users 待创建的用户列表
     * @return 全部成功时返回成功；存在失败时返回失败，string 中包含失败的用户 id（英文逗号分隔）*/
    @Transactional(rollbackFor = Exception.class)
    public RequestResult createUsers(@NonNull HttpSession httpSession, @NonNull List<CreatedUserView> users) {
        Locale locale = Locale.CHINA;
        String adminId = (String) httpSession.getAttribute("userId");
        if (!isAdmin(httpSession)) {
            return new RequestResult(false, 403,
                    msgSource.getMessage("responseMessage.accessDenied", null, locale));
        }

        List<String> failedIds = new ArrayList<>();
        List<CreatedUserView> toCreate = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        for (CreatedUserView user : users) {
            Long id = user.getId();
            if (id == null || seenIds.contains(id) || userMapper.selectById(id) != null
                    || userPasswordMapper.selectById(id) != null) {
                failedIds.add(id == null ? "(空)" : id.toString());
                continue;
            }
            seenIds.add(id);
            toCreate.add(user);
        }

        for (CreatedUserView createdUser : toCreate) {
            Long id = createdUser.getId();
            userMapper.insert(new User(id, emptyToNull(createdUser.getName()),
                    createdUser.getAge(), emptyToNull(createdUser.getEmail())));

            String position = emptyToNull(createdUser.getPosition());
            if (position == null) {
                position = UserPosition.STUDENT.getString();
            }
            String initialPassword = emptyToNull(createdUser.getPassword());
            if (initialPassword == null) {
                initialPassword = id.toString();
            }
            String salt = BasicFunctions.generateSalt();
            String hash = BasicFunctions.hashPassword(initialPassword, salt);
            userPasswordMapper.insert(new UserPassword(id, hash, salt, position));
        }

        if (!failedIds.isEmpty()) {
            String failedList = String.join(",", failedIds);
            BasicFunctions.printTimeHttpSessionAnd(httpSession, adminId,
                    msgSource.getMessage("logMessage.createUsersFailed",
                            new Object[]{failedList}, Locale.CHINA));
            return new RequestResult(false, -1,
                    msgSource.getMessage("responseMessage.createUserFailed",
                            new Object[]{failedList}, locale));
        }
        BasicFunctions.printTimeHttpSessionAnd(httpSession, adminId,
                msgSource.getMessage("logMessage.createUsers",
                        new Object[]{toCreate.size()}, Locale.CHINA));
        return new RequestResult(true, 200,
                msgSource.getMessage("responseMessage.createUserSuccess",
                        new Object[]{toCreate.size()}, locale));
    }

    /**管理员重置指定用户的密码：新密码留空则重置为该用户的 id。重新生成盐并计算哈希。
     * @param httpSession 当前http会话
     * @param userId 被重置密码的用户id
     * @param newPassword 新密码，留空表示重置为账号id
     * @return 重置结果，失败时附带提示信息*/
    public RequestResult resetPassword(@NonNull HttpSession httpSession,
                                       @NonNull Long userId, String newPassword) {
        Locale locale = Locale.CHINA;
        String adminId = (String) httpSession.getAttribute("userId");
        if (!isAdmin(httpSession)) {
            return new RequestResult(false, 403,
                    msgSource.getMessage("responseMessage.accessDenied", null, locale));
        }

        UserPassword userPassword = userPasswordMapper.selectById(userId);
        if (userPassword == null) {
            BasicFunctions.printTimeHttpSessionAnd(httpSession, adminId,
                    msgSource.getMessage("logMessage.resetPasswordUserNotFound",
                            new Object[]{userId}, Locale.CHINA));
            return new RequestResult(false, 404,
                    msgSource.getMessage("responseMessage.userIdNotFoundForReset",
                            new Object[]{userId}, locale));
        }

        String password = emptyToNull(newPassword);
        if (password != null && (password.length() < ChangePasswordService.MIN_PASSWORD_LENGTH
                || password.length() > ChangePasswordService.MAX_PASSWORD_LENGTH)) {
            return new RequestResult(false, -1,
                    msgSource.getMessage("responseMessage.newPasswordLengthIllegal", null, locale));
        }
        if (password == null) {
            password = userId.toString();
        }

        String salt = BasicFunctions.generateSalt();
        String hash = BasicFunctions.hashPassword(password, salt);
        userPasswordMapper.updateById(new UserPassword(userId, hash, salt, userPassword.getPosition()));

        BasicFunctions.printTimeHttpSessionAnd(httpSession, adminId,
                msgSource.getMessage("logMessage.resetPassword",
                        new Object[]{userId}, Locale.CHINA));
        return new RequestResult(true, 200,
                msgSource.getMessage("responseMessage.passwordReset", null, locale));
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
