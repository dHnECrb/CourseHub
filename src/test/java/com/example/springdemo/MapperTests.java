package com.example.springdemo;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.example.springdemo.entity.*;
import com.example.springdemo.mapper.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
//@MapperScan("com.example.springdemo.mapper")
class MapperTests { // 原名：SpringDemoApplicationTests

    @Autowired//也可使用@Resourse
    private UserMapper userMapper;

    @Resource
    private UserPasswordMapper userPasswordMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private CourseSelectMapper courseSelectMapper;

    @Resource
    private UserTaskMapper userTaskMapper;

    @Test
    void contextLoads() {
    }

    @Test
    public void testSelect() {
        System.out.println(("----- selectAll method test ------"));
        List<User> userList = userMapper.selectList(null);
        Assert.isTrue(6 == userList.size(), "");
        userList.forEach(System.out::println);
    }

    @Test
    public void testCourseMapper(){
        List<Course> courses; // =courseMapper.findAll();
        Map<String, Object> selectMap=new HashMap<>();

        if( courseMapper.selectList(null).isEmpty()) {
            courseMapper.insert(new Course(1L, "数据库原理", "2025-2026-2", "专业必修课", true));
            courseMapper.insert(new Course(2L, "Web前端编程技术", "2025-2026-2", "专业选修课", true));
            courseMapper.insert(new Course(3L, "数据结构与算法课程设计", "2025-2026-2", "实践选修课", true));
            courseMapper.insert(new Course(4L, "移动应用开发", "2026-2027-1", "专业选修课", true));
            courseMapper.insert(new Course(5L, "人工智能", "2026-2027-1", "专业选修课", true));
            courseMapper.insert(new Course(6L, "信息管理", "2026-2027-1", "专业选修课", false));
            courseMapper.insert(new Course(7L, "人机交互技术", "2026-2027-1", "专业选修课", true));
        }

        courseMapper.updateById(new Course(2L, "计算机组成与体系结构课程设计", null, "实践选修课", false));
//        courseMapper.deleteById(new Course(1L, null, null,null,false));
        courseMapper.deleteById(1L);

        selectMap.put("term", "2025-2026-2");
        courses=courseMapper.selectByMap(selectMap);
        System.out.println("=================2025-2026-2学期的课程：====================");
        courses.forEach(System.out::println);
        courses=courseMapper.selectList(null);
        System.out.println("=================所有课程：====================");
        courses.forEach(System.out::println);
    }

    @Test
    public void testUser(){
        List<User> userList;
        userList=userMapper.selectList(null);
        userList.forEach(System.out::println);

        userMapper.updateById(new User(11L, "NoName", 25, "noEmail"));
    }

    @Test
    public void testUserPassword(){
        List<UserPassword> userPasswordList;
        userPasswordList=userPasswordMapper.selectList(null);
        userPasswordList.forEach(System.out::println);

        userPasswordMapper.updateById(new UserPassword(11L, "111111", "teacher"));
    }

    @Test
    public void testCreateUser(){
        User user;
        UserPassword userPassword;
        Long id=12L;

        user=new User(id, "Hherern", 20, "hherern@163.com");
        userPassword=new UserPassword(id, "mySystem", "admin");

        userMapper.insert(user);
        userPasswordMapper.insert(userPassword);
        System.out.println(userMapper.selectById(id));
        System.out.println(userPasswordMapper.selectById(id));

    }

    @Test
    public void testTask(){
        List<UserTask> userTasks = userTaskMapper.selectList(null);
        LocalDateTime localDateTime=LocalDateTime.of(2099,12,31,23,59,59);
        if (userTasks.isEmpty()) {
            userTaskMapper.insert(new UserTask("修改密码", "all", "", "/index", LocalDateTime.now(), localDateTime));
            userTaskMapper.insert(new UserTask("预选课", "student", "", "/index", LocalDateTime.now(), localDateTime));
            userTaskMapper.insert(new UserTask("修改密码", "teacher", "", "/login", LocalDateTime.now(), localDateTime));
            userTaskMapper.insert(new UserTask("Change Password", "admin", "", "/login", LocalDateTime.now(), localDateTime));
        }
        else {
            userTasks = userTaskMapper.selectList(null);
            System.out.println("==============所有任务=================");
            userTasks.forEach(System.out::println);

            userTaskMapper.updateById(new UserTask(2106318257863450626L, "登录",
                    "", "/login", LocalDateTime.now(), localDateTime, "admin"));
            userTasks = userTaskMapper.selectList(null);
            System.out.println("==============修改了一个=================");
            userTasks.forEach(System.out::println);

            userTaskMapper.deleteById(2106318257871839234L);
            userTasks = userTaskMapper.selectList(null);
            System.out.println("==============删除了一个=================");
            userTasks.forEach(System.out::println);
        }
    }

    @Test
    public void testCourseSelect(){
        List<CourseSelect> courseSelects=courseSelectMapper.selectList(null);
        Map<String, Object> findByUserMap=new HashMap<>(), findByCourseMap=new HashMap<>(), deleteMap=new HashMap<>();

        if(courseSelects.isEmpty()) {
            courseSelectMapper.insert(new CourseSelect(1L, 2L, LocalDateTime.now()));
            courseSelectMapper.insert(new CourseSelect(2L, 2L, LocalDateTime.now()));
            courseSelectMapper.insert(new CourseSelect(3L, 4L, LocalDateTime.now()));
            courseSelectMapper.insert(new CourseSelect(4L, 2L, LocalDateTime.now()));
            courseSelectMapper.insert(new CourseSelect(1L, 3L, LocalDateTime.now()));
            courseSelectMapper.insert(new CourseSelect(3L, 3L, LocalDateTime.now()));
        }

        courseSelects=courseSelectMapper.selectList(null);
        System.out.println("==============所有选课=================");
        courseSelects.forEach(System.out::println);

        findByUserMap.put("user_id", 2L);
        courseSelects=courseSelectMapper.selectByMap(findByUserMap);
        System.out.println("==============2号学生的选课记录=================");
        courseSelects.forEach(System.out::println);

        findByCourseMap.put("course_id", 2L);
        courseSelects=courseSelectMapper.selectByMap(findByCourseMap);
        System.out.println("==============2号课程的选课记录=================");
        courseSelects.forEach(System.out::println);

        deleteMap.put("user_id", 3L);
        deleteMap.put("course_id", 4L);
        courseSelectMapper.deleteByMap(deleteMap);
        courseSelects=courseSelectMapper.selectList(null);
        System.out.println("==============删除了一条, 选课记录=================");
        courseSelects.forEach(System.out::println);
    }
}





