package com.example.springdemo.dto;

import lombok.Data;

/**管理员新增或修改课程时提交的一行课程信息（来自单个表单或 CSV 导入）*/
@Data
public class ManagedCourseView {
    private Long id;
    private String name;
    private String term;
    private String property;
    private Boolean will_offer;
    private Integer credit;
}
