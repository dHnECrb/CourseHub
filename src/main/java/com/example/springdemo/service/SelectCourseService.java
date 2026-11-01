package com.example.springdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.dto.SelectedCourseView;
import com.example.springdemo.dto.SelectingCourseView;
import com.example.springdemo.entity.Course;
import com.example.springdemo.entity.CourseSelect;
import com.example.springdemo.entity.YearTerm;
import com.example.springdemo.mapper.CourseMapper;
import com.example.springdemo.mapper.CourseSelectMapper;
import com.example.springdemo.util.BasicFunctions;
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

    private boolean hasPermission(@NonNull HttpSession httpSession){
        return userTaskService.tryGoTask(httpSession, SITE).isSucceed();
    }

    public RequestResult index(@NonNull HttpSession httpSession){
        Locale locale = Locale.CHINA;
        String taskName="预选课";
        return userTaskService.goTask(httpSession,taskName, SITE, locale);
    }

    public List<Course> getAllCourse(@NonNull HttpSession httpSession){
        if(!hasPermission(httpSession)){
            return List.of();
        }
        return courseMapper.selectList(null);
    }

    /**获取当前学期的课程，可以选择是否要过滤可能不开课的课程
     * @param httpSession 当前http会话
     * @param filterWillOffer 是否要过滤可能不开课*/
    public List<Course> getCurrentTermCourse(@NonNull HttpSession httpSession, boolean filterWillOffer){
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if(!hasPermission(httpSession)){
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
     * @param httpSession 当前http会话
     * @param selectedCourseIdList 增加的选课
     * @param canceledCourseIdList 取消的选课
     * */
    public RequestResult changeSelectCourses(@NonNull HttpSession httpSession, List<Long> selectedCourseIdList,
                                    List<Long> canceledCourseIdList){
        Long userId=Long.parseLong((String)httpSession.getAttribute("userId"));
        if(!hasPermission(httpSession)){
            return new RequestResult(false, 403, null);
        }
        for(Long courseId:selectedCourseIdList) {
            courseSelectMapper.insert(new CourseSelect(userId, courseId, LocalDateTime.now(), YearTerm.now().toString()));
        }

        if(!canceledCourseIdList.isEmpty()){
            LambdaQueryWrapper<CourseSelect> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CourseSelect::getUser_id, userId)
                    .in(CourseSelect::getCourse_id, canceledCourseIdList);
            courseSelectMapper.delete(wrapper);
        }
        BasicFunctions.printTimeHttpSessionAnd(httpSession, userId.toString(), "增加课程"+selectedCourseIdList+"删除课程"+canceledCourseIdList);
        return new RequestResult(true, 200, null);
    }

    /**查询当前学生选的所有课程及其修读状态
     * @param httpSession 当前http会话
     * @return 已选课程视图列表（含修读状态），无权限时返回空列表*/
    public List<SelectedCourseView> getSelectedCourses(@NonNull HttpSession httpSession){
        Long userId=Long.parseLong((String)httpSession.getAttribute("userId"));
        if(!hasPermission(httpSession)){
            return List.of();
        }
        return courseSelectMapper.selectSelectedCourses(userId, YearTerm.now().toString());
    }

    /**查询当前学生所有可选的课程以及选择情况
     * @param httpSession 当前http会话
     * @param filterWillOffer 是否筛选可能不开课的课程
     * @return 下一学期可选课程视图列表，无权限时返回空列表*/
    public List<SelectingCourseView> getCoursesToSelect(@NonNull HttpSession httpSession, boolean filterWillOffer){
        Long userId=Long.parseLong((String)httpSession.getAttribute("userId"));
        if(!hasPermission(httpSession)){
            return List.of();
        }
        return courseSelectMapper.selectCoursesToSelect(userId, YearTerm.now().next().toString(), filterWillOffer);
    }


}
