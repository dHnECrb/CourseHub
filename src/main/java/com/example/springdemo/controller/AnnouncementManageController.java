package com.example.springdemo.controller;

import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.service.AnnouncementService;
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

@RestController
@RequestMapping("/admin/announcement_manage")
public class AnnouncementManageController {

    @Resource
    private AnnouncementService announcementService;

    @GetMapping
    public ModelAndView index(@NonNull HttpSession httpSession,
                              @NonNull HttpServletResponse response) throws IOException {
        RequestResult requestResult = announcementService.index(httpSession);
        if (requestResult.isSucceed()) {
            ModelAndView modelAndView = new ModelAndView("announcement_manage");
            modelAndView.addObject("latestAnnouncement", announcementService.getLatestAnnouncement());
            return modelAndView;
        }
        response.sendError(requestResult.code(), requestResult.string());
        return null;
    }

    @PostMapping("/publish")
    public RequestResult publish(@NonNull HttpSession httpSession,
                                 @RequestParam(required = false) String title,
                                 @RequestParam(required = false) String content) {
        return announcementService.publish(httpSession, title, content);
    }
}
