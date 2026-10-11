package com.example.springdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springdemo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**查询指定职位的所有用户（user 表与 userPassword 表连接，按用户编号升序）
     * @param position 职位字符串（如 student、admin、teacher）
     * @return 该职位的用户列表*/
    List<User> selectByPosition(@Param("position") String position);
//    User findById(Long id);
//
//    List<User> findAll();
//
//    int addUser(User user);
//
//    int update(User user);

//    int delUser(User user);


}
