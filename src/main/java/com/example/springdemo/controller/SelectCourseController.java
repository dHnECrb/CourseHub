package com.example.springdemo.controller;

import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.service.SelectCourseService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@RestController
@RequestMapping("/student/select_course")
public class SelectCourseController {
    @Resource
    private SelectCourseService selectCourseService;

    @GetMapping
    public ModelAndView index(@NonNull HttpSession httpSession, HttpServletResponse response) throws IOException {
        RequestResult requestResult=selectCourseService.index(httpSession);

        if(requestResult.isSucceed()){
            return new ModelAndView("select_course");
        }
        response.sendError(requestResult.code(), requestResult.string());
        return null;
    }
}
