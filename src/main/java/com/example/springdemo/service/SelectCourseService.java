package com.example.springdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springdemo.entity.Course;
import com.example.springdemo.entity.CourseSelect;
import com.example.springdemo.entity.UserTask;
import com.example.springdemo.entity.YearTerm;
import com.example.springdemo.mapper.CourseMapper;
import com.example.springdemo.mapper.CourseSelectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class SelectCourseService {
    @Resource
    private CourseMapper courseMapper;

    @Resource
    private CourseSelectMapper courseSelectMapper;

    @Resource
    private UserTaskService userTaskService;

    @Resource
    private MessageSource msgSource;

    private static final String SITE ="/student/select_course";

    public RequestResult index(@NonNull HttpSession httpSession){
        Locale locale = Locale.CHINA;
        String taskName="预选课";
        return userTaskService.goTask(httpSession,taskName, SITE, locale);
    }

    public List<Course> getAllCourse(@NonNull HttpSession httpSession){
        if(!userTaskService.tryGoTask(httpSession, SITE).isSucceed()){
            return List.of();
        }
        return courseMapper.selectList(null);
    }

    public List<Course> getCurrentTermCourse(@NonNull HttpSession httpSession, boolean filterWillOffer){
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if(!userTaskService.tryGoTask(httpSession, SITE).isSucceed()){
            return List.of();
        }
        if(filterWillOffer){
            wrapper.eq(Course::getTerm, YearTerm.parse(LocalDate.now()).toString())
                    .eq(Course::isWill_offer, true);
        }
        else {
            wrapper.eq(Course::getTerm, YearTerm.parse(LocalDate.now()).toString());
        }
        return courseMapper.selectList(wrapper);
    }

    /**批量更新选课
     * @param courseIdList 课程号列表
     * @param insertOrDelete 新选课(true)还是删除选课(false)*/
    public void ChangeSelectCourses(@NonNull HttpSession httpSession, List<Long> courseIdList, boolean insertOrDelete){
        Long userId=Long.parseLong((String)httpSession.getAttribute("userId"));
        if(insertOrDelete){
            for(Long courseId:courseIdList) {
                courseSelectMapper.insert(new CourseSelect(userId, courseId, LocalDateTime.now()));
            }
        }
        else{
            courseSelectMapper.deleteByIds(courseIdList);
        }
    }
}
