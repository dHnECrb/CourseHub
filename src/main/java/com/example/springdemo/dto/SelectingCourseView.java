package com.example.springdemo.dto;

import lombok.Data;

@Data
public class SelectingCourseView {
    private Long    courseId;   // 课程编号
    private String  courseName; // 课程名称
    private int     credit;     // 课程学分
    private String  property;   // 课程性质
    private String  term;       // 开课学年学期
    private String  will_offer; // 能否开课
    private boolean is_chosen;  // 是否选择

    public SelectingCourseView(Long courseId, String courseName, int credit, String property, String term, String will_offer, boolean is_chosen) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.credit = credit;
        this.property = property;
        this.term = term;
        this.will_offer = will_offer;
        this.is_chosen = is_chosen;
    }
}
