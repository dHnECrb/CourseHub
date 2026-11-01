package com.example.springdemo.controller;

import com.example.springdemo.dto.OnlineCountsView;
import com.example.springdemo.dto.OnlineLimitsView;
import com.example.springdemo.dto.OnlineUserView;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.service.OnlineUserManageService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/admin/online_user_manage")
public class OnlineUserManageController {

    @Resource
    private OnlineUserManageService onlineUserManageService;

    @GetMapping
    public ModelAndView index(@NonNull HttpSession httpSession, @NonNull HttpServletResponse response) throws IOException {
        RequestResult requestResult = onlineUserManageService.index(httpSession);

        if (requestResult.isSucceed()) {
            return new ModelAndView("online_user_manage");
        }
        response.sendError(requestResult.code(), requestResult.string());
        return null;
    }

    @GetMapping("/get_counts")
    public OnlineCountsView getCounts(@NonNull HttpSession httpSession) {
        return onlineUserManageService.getCounts(httpSession);
    }

    @GetMapping("/get_limits")
    public OnlineLimitsView getLimits(@NonNull HttpSession httpSession) {
        return onlineUserManageService.getLimits(httpSession);
    }

    @PostMapping("/set_limits")
    public RequestResult setLimits(@NonNull HttpSession httpSession,
                                   @RequestParam int maxOnlineUserCount,
                                   @RequestParam int maxSessionCountPerUser) {
        return onlineUserManageService.setLimits(httpSession, maxOnlineUserCount, maxSessionCountPerUser);
    }

    @GetMapping("/get_users")
    public List<OnlineUserView> getUsers(@NonNull HttpSession httpSession) {
        return onlineUserManageService.getUsers(httpSession);
    }
}
