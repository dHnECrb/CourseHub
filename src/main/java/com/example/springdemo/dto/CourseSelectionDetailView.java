package com.example.springdemo.dto;

import com.example.springdemo.entity.Course;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**学生选课明细矩阵：课程列表（列）+ 每个学生的选择情况（行）*/
@Data
@NoArgsConstructor
public class CourseSelectionDetailView {
    private List<Course> courses;  // 列：本学期的课程
    private List<StudentSelectionDetailView> students; // 行：每个学生的选课情况

    public CourseSelectionDetailView(List<Course> courses, List<StudentSelectionDetailView> students) {
        this.courses = courses;
        this.students = students;
    }
}
