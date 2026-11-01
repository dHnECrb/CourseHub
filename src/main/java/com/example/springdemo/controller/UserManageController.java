package com.example.springdemo.controller;

import com.example.springdemo.dto.CreatedUserView;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.service.UserManageService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/admin/user_manage")
public class UserManageController {

    @Resource
    private UserManageService userManageService;

    @GetMapping
    public ModelAndView index(@NonNull HttpSession httpSession, @NonNull HttpServletResponse response) throws IOException {
        RequestResult requestResult = userManageService.index(httpSession);

        if (requestResult.isSucceed()) {
            return new ModelAndView("user_manage");
        }
        response.sendError(requestResult.code(), requestResult.string());
        return null;
    }

    @PostMapping("/create")
    public RequestResult createUsers(@NonNull HttpSession httpSession,
                                     @NonNull @RequestBody List<CreatedUserView> users) {
        return userManageService.createUsers(httpSession, users);
    }

    @PostMapping("/reset_password")
    public RequestResult resetPassword(@NonNull HttpSession httpSession,
                                       @RequestParam @NonNull Long userId,
                                       @RequestParam(required = false) String newPassword) {
        return userManageService.resetPassword(httpSession, userId, newPassword);
    }
}
