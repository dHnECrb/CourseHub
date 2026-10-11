package com.example.springdemo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**用于在前端展示每门课程的选课学生汇总信息*/
@Data
@NoArgsConstructor
public class CourseSelectionSummaryView {
    private Long   courseId;      // 课程编号
    private String courseName;    // 课程名称
    private String studentNames;  // 选课学生姓名（顿号分隔，无人选课时为 null）
    private int    studentCount;  // 选课总人数

    public CourseSelectionSummaryView(Long courseId, String courseName, String studentNames, int studentCount) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.studentNames = studentNames;
        this.studentCount = studentCount;
    }
}
