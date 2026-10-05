package com.example.springdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springdemo.entity.Course;
import com.example.springdemo.entity.CourseSelect;
import com.example.springdemo.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CourseSelectMapper extends BaseMapper<CourseSelect> {
//    int newCourseSelect(CourseSelect courseSelect);
//
//    int delete(CourseSelect courseSelect);
//
//    List<CourseSelect> getAll();
//
//    List<CourseSelect> findByCourse(Course course);
//
//    List<CourseSelect> findByUser(User user);
}
