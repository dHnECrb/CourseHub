package com.example.springdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springdemo.dto.SelectedCourseView;
import com.example.springdemo.dto.SelectingCourseView;
import com.example.springdemo.entity.Course;
import com.example.springdemo.entity.CourseSelect;
import com.example.springdemo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseSelectMapper extends BaseMapper<CourseSelect> {

    /**连接查询某学生选的所有课程及其修读状态
     * @param userId 学生编号
     * @param currentTerm 当前学年学期（格式 YYYY-YYYY-1/2），用于判定修读状态*/
    List<SelectedCourseView> selectSelectedCourses(@Param("userId") Long userId, @Param("currentTerm") String currentTerm);

    /**连接查询给定学期开课的课程及某学生的选择情况（LEFT JOIN，未选的课程也会返回）
     * @param userId 学生编号
     * @param term 开课学年学期（格式 YYYY-YYYY-1/2）
     * @param filterWillOffer 是否只返回确定开课的课程
     * @return 可选课程视图列表，is_chosen 表示该学生是否已选*/
    List<SelectingCourseView> selectCoursesToSelect(@Param("userId") Long userId, @Param("term") String term, @Param("filterWillOffer") boolean filterWillOffer);
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
