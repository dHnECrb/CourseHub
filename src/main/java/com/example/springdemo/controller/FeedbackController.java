package com.example.springdemo.controller;

import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.service.FeedbackService;
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
@RequestMapping("/feedback")
public class FeedbackController {

    @Resource
    private FeedbackService feedbackService;

    @GetMapping
    public ModelAndView index(@NonNull HttpSession httpSession,
                              @NonNull HttpServletResponse response) throws IOException {
        RequestResult requestResult = feedbackService.index(httpSession);
        if (requestResult.isSucceed()) {
            ModelAndView modelAndView = new ModelAndView("feedback");
            modelAndView.addObject("feedbackList",
                    feedbackService.getUserFeedback((String) httpSession.getAttribute("userId")));
            return modelAndView;
        }
        response.sendError(requestResult.code(), requestResult.string());
        return null;
    }

    @PostMapping("/submit")
    public RequestResult submit(@NonNull HttpSession httpSession,
                                @RequestParam(required = false) String content) {
        return feedbackService.submit(httpSession, content);
    }

    @PostMapping("/delete")
    public RequestResult delete(@NonNull HttpSession httpSession,
                                @RequestParam(required = false) Long feedbackId) {
        return feedbackService.delete(httpSession, feedbackId);
    }
}
