package com.example.springdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springdemo.dto.ManagedCourseView;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.entity.Course;
import com.example.springdemo.entity.CourseSelect;
import com.example.springdemo.entity.UserPosition;
import com.example.springdemo.entity.YearTerm;
import com.example.springdemo.mapper.CourseMapper;
import com.example.springdemo.mapper.CourseSelectMapper;
import com.example.springdemo.util.BasicFunctions;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CourseManageService {

    @Resource
    private MessageSource msgSource;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private CourseSelectMapper courseSelectMapper;

    private boolean isAdmin(@NonNull HttpSession httpSession) {
        Object position = httpSession.getAttribute("userPosition");
        return position instanceof UserPosition && position == UserPosition.ADMIN;
    }

    public RequestResult checkPermission(@NonNull HttpSession httpSession) {
        Locale locale = Locale.CHINA;
        if (httpSession.getAttribute("userId") == null) {
            return new RequestResult(false, 401,
                    msgSource.getMessage("responseMessage.didNotLogin", null, locale));
        }
        if (!isAdmin(httpSession)) {
            return new RequestResult(false, 403,
                    msgSource.getMessage("responseMessage.accessDenied", null, locale));
        }
        return new RequestResult(true, 200, null);
    }

    /**管理员进入课程管理页面。*/
    public RequestResult index(@NonNull HttpSession httpSession) {
        return checkPermission(httpSession);
    }

    /**按可选条件查询课程，空条件不参与筛选。*/
    public List<Course> getCourses(Long id, String name, String term, String property, Boolean willOffer) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(id != null, Course::getId, id)
                .like(name != null && !name.isBlank(), Course::getName, name == null ? null : name.trim())
                .eq(term != null && !term.isBlank(), Course::getTerm, term == null ? null : term.trim())
                .eq(property != null && !property.isBlank(), Course::getProperty,
                        property == null ? null : property.trim())
                .eq(willOffer != null, Course::isWill_offer, willOffer)
                .orderByAsc(Course::getId);
        return courseMapper.selectList(wrapper);
    }

    /**批量新增课程，合法行正常插入，非法行跳过并返回原因。*/
    @Transactional(rollbackFor = Exception.class)
    public RequestResult createCourses(@NonNull HttpSession httpSession,
                                       @NonNull List<ManagedCourseView> courses) {
        RequestResult permission = checkPermission(httpSession);
        if (!permission.isSucceed()) {
            return permission;
        }

        List<String> failures = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        int successCount = 0;
        for (ManagedCourseView course : courses) {
            String error = validateCourse(course);
            if (error != null) {
                failures.add(describeFailure(course == null ? null : course.getId(), error));
                continue;
            }
            Long id = course.getId();
            if (!seenIds.add(id)) {
                failures.add(describeFailure(id, "本批次中课程编号重复"));
                continue;
            }
            if (courseMapper.selectById(id) != null) {
                failures.add(describeFailure(id, "课程编号已存在"));
                continue;
            }
            courseMapper.insert(toCourse(course));
            successCount++;
        }
        return batchResult(httpSession, "新增课程", successCount, failures);
    }

    /**批量修改课程，课程编号不可更改，不存在的课程跳过。*/
    @Transactional(rollbackFor = Exception.class)
    public RequestResult updateCourses(@NonNull HttpSession httpSession,
                                       @NonNull List<ManagedCourseView> courses) {
        RequestResult permission = checkPermission(httpSession);
        if (!permission.isSucceed()) {
            return permission;
        }

        List<String> failures = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        int successCount = 0;
        for (ManagedCourseView course : courses) {
            String error = validateCourse(course);
            if (error != null) {
                failures.add(describeFailure(course == null ? null : course.getId(), error));
                continue;
            }
            Long id = course.getId();
            if (!seenIds.add(id)) {
                failures.add(describeFailure(id, "本批次中课程编号重复"));
                continue;
            }
            if (courseMapper.selectById(id) == null) {
                failures.add(describeFailure(id, "课程不存在"));
                continue;
            }
            courseMapper.updateById(toCourse(course));
            successCount++;
        }
        return batchResult(httpSession, "修改课程", successCount, failures);
    }

    /**批量删除课程；已有选课记录的课程保留，避免破坏选课历史。*/
    @Transactional(rollbackFor = Exception.class)
    public RequestResult deleteCourses(@NonNull HttpSession httpSession,
                                       @NonNull List<Long> courseIds) {
        RequestResult permission = checkPermission(httpSession);
        if (!permission.isSucceed()) {
            return permission;
        }

        List<String> failures = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        int successCount = 0;
        for (Long id : courseIds) {
            if (id == null || id <= 0) {
                failures.add(describeFailure(id, "课程编号为空或不合法"));
                continue;
            }
            if (!seenIds.add(id)) {
                failures.add(describeFailure(id, "本批次中课程编号重复"));
                continue;
            }
            if (courseMapper.selectById(id) == null) {
                failures.add(describeFailure(id, "课程不存在"));
                continue;
            }
            LambdaQueryWrapper<CourseSelect> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CourseSelect::getCourse_id, id);
            if (courseSelectMapper.selectCount(wrapper) > 0) {
                failures.add(describeFailure(id, "已有选课记录，无法删除"));
                continue;
            }
            courseMapper.deleteById(id);
            successCount++;
        }
        return batchResult(httpSession, "删除课程", successCount, failures);
    }

    private String validateCourse(ManagedCourseView course) {
        if (course == null) {
            return "课程数据为空";
        }
        if (course.getId() == null || course.getId() <= 0) {
            return "课程编号为空或不合法";
        }
        if (course.getName() == null || course.getName().isBlank()) {
            return "课程名不能为空";
        }
        if (course.getProperty() == null || course.getProperty().isBlank()) {
            return "课程性质不能为空";
        }
        if (course.getWill_offer() == null) {
            return "是否开课不能为空";
        }
        if (course.getCredit() == null || course.getCredit() <= 0) {
            return "学分必须为正整数";
        }
        String term = course.getTerm();
        if (term == null || !term.trim().matches("\\d{4}-\\d{4}-[12]")) {
            return "学期格式应为YYYY-YYYY-1或YYYY-YYYY-2";
        }
        try {
            YearTerm.parse(term.trim());
        } catch (RuntimeException e) {
            return "学年或学期不合法";
        }
        return null;
    }

    private Course toCourse(ManagedCourseView view) {
        return new Course(view.getId(), view.getName().trim(), view.getTerm().trim(),
                view.getProperty().trim(), view.getWill_offer(), view.getCredit());
    }

    private String describeFailure(Long id, String reason) {
        return (id == null ? "(空)" : id) + "：" + reason;
    }

    private RequestResult batchResult(HttpSession httpSession, String operation,
                                      int successCount, List<String> failures) {
        Locale locale = Locale.CHINA;
        String failureDetail = String.join("；", failures);
        String message;
        if (failures.isEmpty()) {
            message = msgSource.getMessage("responseMessage.manageCoursesSuccess",
                    new Object[]{operation, Integer.toString(successCount)}, locale);
        } else {
            message = msgSource.getMessage("responseMessage.manageCoursesWithFailures",
                    new Object[]{operation, Integer.toString(successCount), Integer.toString(failures.size()),
                            failureDetail}, locale);
        }
        String adminId = (String) httpSession.getAttribute("userId");
        BasicFunctions.printTimeHttpSessionAnd(httpSession, adminId,
                msgSource.getMessage("logMessage.manageCourses",
                        new Object[]{operation, Integer.toString(successCount), Integer.toString(failures.size()),
                                failureDetail}, Locale.CHINA));
        return new RequestResult(failures.isEmpty(), failures.isEmpty() ? 200 : -1, message);
    }
}
