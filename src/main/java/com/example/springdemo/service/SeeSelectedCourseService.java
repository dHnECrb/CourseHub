package com.example.springdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springdemo.dto.CourseSelectionDetailView;
import com.example.springdemo.dto.CourseSelectionSummaryView;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.dto.StudentSelectionDetailView;
import com.example.springdemo.entity.Course;
import com.example.springdemo.entity.CourseSelect;
import com.example.springdemo.entity.User;
import com.example.springdemo.entity.UserPosition;
import com.example.springdemo.entity.YearTerm;
import com.example.springdemo.mapper.CourseMapper;
import com.example.springdemo.mapper.CourseSelectMapper;
import com.example.springdemo.mapper.UserMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SeeSelectedCourseService {

    @Resource
    private MessageSource msgSource;

    @Resource
    private UserTaskService userTaskService;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private CourseSelectMapper courseSelectMapper;

    @Resource
    private UserMapper userMapper;

    private static final String SITE="/admin/course_select_result";

    private boolean hasPermission(@NonNull HttpSession httpSession){
        return userTaskService.tryGoTask(httpSession, SITE).isSucceed();
    }

    public RequestResult index(@NonNull HttpSession httpSession){
        Locale locale=Locale.CHINA;
        if(httpSession.getAttribute("userId")==null) {
            return new RequestResult(false, 401,
                    msgSource.getMessage("responseMessage.didNotLogin",
                            null,
                            locale));
        }
        return userTaskService.goTask(httpSession,
                "选课查看", SITE, locale);
    }

    /**按课程统计下学期预选的学生汇总信息
     * @param httpSession 当前http会话
     * @return 每门课程的选课学生姓名与总人数，无权限时返回空列表*/
    public List<CourseSelectionSummaryView> getSummary(@NonNull HttpSession httpSession){
        if(!hasPermission(httpSession)){
            return List.of();
        }
        return courseSelectMapper.selectCourseSelectionSummary(YearTerm.now().next().toString());
    }

    /**查询下学期学生选课的明细矩阵（学生×课程）
     * @param httpSession 当前http会话
     * @return 课程列表与每个学生的已选课程编号，无权限时返回 null*/
    public CourseSelectionDetailView getDetail(@NonNull HttpSession httpSession){
        if(!hasPermission(httpSession)){
            return null;
        }
        String nextTerm = YearTerm.now().next().toString();
        String nowTerm=YearTerm.now().toString();

        LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.eq(Course::getTerm, nextTerm).orderByAsc(Course::getId);
        List<Course> courses = courseMapper.selectList(courseWrapper);

        List<User> students = userMapper.selectByPosition(UserPosition.STUDENT.getString());

        LambdaQueryWrapper<CourseSelect> selectWrapper = new LambdaQueryWrapper<>();
        selectWrapper.eq(CourseSelect::getSelect_term, nowTerm);
        Map<Long, List<Long>> selectedByStudent = courseSelectMapper.selectList(selectWrapper).stream()
                .collect(Collectors.groupingBy(CourseSelect::getUser_id,
                        Collectors.mapping(CourseSelect::getCourse_id, Collectors.toList())));

        List<StudentSelectionDetailView> studentViews = students.stream()
                .map(u -> new StudentSelectionDetailView(u.getId(), u.getName(),
                        selectedByStudent.getOrDefault(u.getId(), List.of())))
                .toList();
        return new CourseSelectionDetailView(courses, studentViews);
    }
}
