package com.example.springdemo.controller;

import com.example.springdemo.dto.CourseSelectionDetailView;
import com.example.springdemo.dto.CourseSelectionSummaryView;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.service.SeeSelectedCourseService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/admin/course_select_result")
public class SeeSelectedCourseController {

    @Resource
    private SeeSelectedCourseService seeSelectedCourseService;

    @GetMapping
    public ModelAndView index(@NonNull HttpSession httpSession, @NonNull HttpServletResponse response) throws IOException {
        RequestResult requestResult=seeSelectedCourseService.index(httpSession);

        if(requestResult.isSucceed()){
            return new ModelAndView("course_select_result");
        }
        response.sendError(requestResult.code(), requestResult.string());
        return null;
    }

    @GetMapping("/get_summary")
    public List<CourseSelectionSummaryView> getSummary(@NonNull HttpSession httpSession){
        return seeSelectedCourseService.getSummary(httpSession);
    }

    @GetMapping("/get_detail")
    public CourseSelectionDetailView getDetail(@NonNull HttpSession httpSession){
        return seeSelectedCourseService.getDetail(httpSession);
    }
}
