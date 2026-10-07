package com.example.springdemo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**用于在前端展示已修和正在修读的课程*/
@Data
@NoArgsConstructor
public class SelectedCourseView {
    private Long    courseId;   // 课程编号
    private String  courseName; // 课程名称
    private int     credit;     // 课程学分
    private String  property;   // 课程性质
    private String  term;       // 开课学年学期
    private String  status;     // 修读状态（已修/正在修读）

    public SelectedCourseView(Long courseId, String courseName, int credit, String property, String term, String status) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.credit = credit;
        this.property = property;
        this.term = term;
        this.status = status;
    }
}
