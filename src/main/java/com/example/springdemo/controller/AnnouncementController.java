package com.example.springdemo.controller;

import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.service.AnnouncementService;
import com.example.springdemo.service.UserTaskService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.Locale;

@RestController
@RequestMapping("/announcement")
public class AnnouncementController {

    @Resource
    private AnnouncementService announcementService;

    @Resource
    private UserTaskService userTaskService;

    @GetMapping
    public ModelAndView index(@NonNull HttpSession httpSession,
                              @NonNull HttpServletResponse response) throws IOException {
        RequestResult requestResult = userTaskService.goTask(httpSession,
                "全部公告", "/announcement", Locale.CHINA);
        if (requestResult.isSucceed()) {
            ModelAndView modelAndView = new ModelAndView("announcement");
            modelAndView.addObject("announcements", announcementService.getAllAnnouncements());
            return modelAndView;
        }
        response.sendError(requestResult.code(), requestResult.string());
        return null;
    }
}
