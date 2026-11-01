package com.example.springdemo.controller;

import com.example.springdemo.dto.ChangePasswordRequest;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.service.ChangePasswordService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@RestController
@RequestMapping("/change_password")
public class ChangePasswordController {

    @Resource
    private ChangePasswordService changePasswordService;

    @GetMapping
    public ModelAndView index(@NonNull HttpSession httpSession, @NonNull HttpServletResponse response) throws IOException {
        RequestResult requestResult = changePasswordService.index(httpSession);

        if (requestResult.isSucceed()) {
            ModelAndView modelAndView = new ModelAndView("change_password");
            modelAndView.addObject("userId", httpSession.getAttribute("userId"));
            return modelAndView;
        }
        response.sendError(requestResult.code(), requestResult.string());
        return null;
    }

    @PostMapping("/submit")
    public RequestResult submit(@NonNull HttpSession httpSession, @NonNull @RequestBody ChangePasswordRequest request) {
        return changePasswordService.changePassword(httpSession, request.oldPassword(), request.newPassword());
    }
}
