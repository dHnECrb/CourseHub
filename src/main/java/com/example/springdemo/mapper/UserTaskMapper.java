package com.example.springdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springdemo.entity.UserTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserTaskMapper extends BaseMapper<UserTask> {
//    List<UserTask> getAll();
//
//    int addTask(UserTask userTask);
//
//    int update(UserTask userTask);
//
//    int delete(UserTask userTask);
}
