package com.example.springdemo.controller;

import com.example.springdemo.dto.ManagedCourseView;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.service.CourseManageService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/admin/course_manage")
public class CourseManageController {

    @Resource
    private CourseManageService courseManageService;

    @GetMapping
    public ModelAndView index(@NonNull HttpSession httpSession, @NonNull HttpServletResponse response) throws IOException {
        RequestResult requestResult = courseManageService.index(httpSession);
        if (requestResult.isSucceed()) {
            return new ModelAndView("course_manage");
        }
        response.sendError(requestResult.code(), requestResult.string());
        return null;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@NonNull HttpSession httpSession,
                                  @RequestParam(required = false) Long id,
                                  @RequestParam(required = false) String name,
                                  @RequestParam(required = false) String term,
                                  @RequestParam(required = false) String property,
                                  @RequestParam(required = false) Boolean willOffer) {
        RequestResult permission = courseManageService.checkPermission(httpSession);
        if (!permission.isSucceed()) {
            return ResponseEntity.status(permission.code()).body(permission);
        }
        return ResponseEntity.ok(courseManageService.getCourses(id, name, term, property, willOffer));
    }

    @PostMapping("/create")
    public RequestResult createCourses(@NonNull HttpSession httpSession,
                                       @NonNull @RequestBody List<ManagedCourseView> courses) {
        return courseManageService.createCourses(httpSession, courses);
    }

    @PostMapping("/update")
    public RequestResult updateCourses(@NonNull HttpSession httpSession,
                                       @NonNull @RequestBody List<ManagedCourseView> courses) {
        return courseManageService.updateCourses(httpSession, courses);
    }

    @PostMapping("/delete")
    public RequestResult deleteCourses(@NonNull HttpSession httpSession,
                                       @NonNull @RequestBody List<Long> courseIds) {
        return courseManageService.deleteCourses(httpSession, courseIds);
    }
}
