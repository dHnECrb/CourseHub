package com.example.springdemo.controller;

import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.dto.SelectedAndCanceledCourses;
import com.example.springdemo.dto.SelectedCourseView;
import com.example.springdemo.dto.SelectingCourseView;
import com.example.springdemo.service.SelectCourseService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/student/select_course")
public class SelectCourseController {
    @Resource
    private SelectCourseService selectCourseService;

    @GetMapping
    public ModelAndView index(@NonNull HttpSession httpSession, @NonNull HttpServletResponse response) throws IOException {
        RequestResult requestResult=selectCourseService.index(httpSession);

        if(requestResult.isSucceed()){
            return new ModelAndView("select_course");
        }
        response.sendError(requestResult.code(), requestResult.string());
        return null;
    }

    @GetMapping("/get_selected")
    public List<SelectedCourseView> getSelectedCourses(@NonNull HttpSession httpSession){
        return selectCourseService.getSelectedCourses(httpSession);
    }

    @GetMapping("/get_to_select")
    public List<SelectingCourseView> getCoursesToSelect(@NonNull HttpSession httpSession, boolean filterWillOffer){
        return selectCourseService.getCoursesToSelect(httpSession, filterWillOffer);
    }

    @PostMapping("/submit")
    public boolean submit(@NonNull HttpSession httpSession, @NonNull HttpServletResponse response,
                          @RequestBody SelectedAndCanceledCourses selectedAndCanceledCourses) throws IOException {
        RequestResult requestResult=selectCourseService.changeSelectCourses(httpSession,
                selectedAndCanceledCourses.getSelect(),
                selectedAndCanceledCourses.getCancel());
        if(requestResult.isSucceed()){
            return true;
        }
        if(requestResult.code()==200){
            return false;
        }
        response.sendError(requestResult.code());
        return false;
    }
}
