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
    private int credit;         // 课程学分

    public Course(Long id) {
        this.id = id;
    }

    /** 之前数据库没有学分一列，后来加上了
     * @deprecated 请使用{@link #Course(Long id, String name, String term,
     * String property, boolean will_offer, int credit)} 代替*/
    @Deprecated
    public Course(Long id, String name, String term, String property, boolean will_offer) {
        this.id = id;
        this.name = name;
        this.term = term;
        this.property = property;
        this.will_offer = will_offer;
    }

    public Course(Long id, String name, String term, String property, boolean will_offer, int credit) {
        this.id = id;
        this.name = name;
        this.term = term;
        this.property = property;
        this.will_offer = will_offer;
        this.credit = credit;
    }
}
