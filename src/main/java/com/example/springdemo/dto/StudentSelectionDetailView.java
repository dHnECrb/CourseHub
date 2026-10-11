package com.example.springdemo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**用于在前端展示学生选课明细矩阵中的一行：学生对每门课是否选择*/
@Data
@NoArgsConstructor
public class StudentSelectionDetailView {
    private Long       studentId;         // 学生编号
    private String     studentName;       // 学生姓名
    private List<Long> selectedCourseIds; // 该学生已选的课程编号列表（一门都没选时为空列表）

    public StudentSelectionDetailView(Long studentId, String studentName, List<Long> selectedCourseIds) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.selectedCourseIds = selectedCourseIds;
    }
}
