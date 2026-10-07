package com.example.springdemo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`select_course`")
public class CourseSelect {
    private Long user_id;               // 用户（学生）编号
    private Long course_id;             // 课程编号
    private LocalDateTime select_time;  // 选课时间
    private String select_term;         // 选课学期


    @Deprecated
    public CourseSelect(Long user_id, Long course_id, LocalDateTime select_time) {
        this.user_id = user_id;
        this.course_id = course_id;
        this.select_time = select_time;
    }

    public CourseSelect(Long user_id, Long course_id, LocalDateTime select_time, String select_term) {
        this.user_id = user_id;
        this.course_id = course_id;
        this.select_time = select_time;
        this.select_term = select_term;
    }

    public CourseSelect(Long user_id, Long course_id) {
        this.user_id = user_id;
        this.course_id = course_id;
    }

    public Long getUser_id() {
        return user_id;
    }

    public Long getCourse_id() {
        return course_id;
    }

    public LocalDateTime getSelect_time() {
        return select_time;
    }
}
