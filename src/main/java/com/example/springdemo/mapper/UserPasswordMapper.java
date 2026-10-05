package com.example.springdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springdemo.entity.UserPassword;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserPasswordMapper extends BaseMapper<UserPassword> {
//    List<UserPassword> findById(Long id);
//
//    List<UserPassword> findAll();

//    @Insert("INSERT INTO userPassword(username, password) VALUES(#{username}, #{password})")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
//    int addUserPassword(UserPassword userPassword);
//
//    int update(UserPassword userPassword);

//    @Delete("DELETE FROM userPassword WHERE id=#{id}")
//    int delete(Long id);

}
