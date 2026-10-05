package com.example.springdemo.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("`course`")
public class Course {
    private Long id;            // 课程编号
    private String name;        // 课程名
    private String term;        // 学年学期
    private String property;    // 课程性质
    private boolean will_offer; // 是否开课

    public Course(Long id) {
        this.id = id;
    }

    public Course(Long id, String name, String term, String property, boolean will_offer) {
        this.id = id;
        this.name = name;
        this.term = term;
        this.property = property;
        this.will_offer = will_offer;
    }

}
