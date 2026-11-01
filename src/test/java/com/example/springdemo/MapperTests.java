package com.example.springdemo;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.example.springdemo.dto.SelectedCourseView;
import com.example.springdemo.dto.SelectingCourseView;
import com.example.springdemo.entity.*;
import com.example.springdemo.mapper.*;
import com.example.springdemo.util.BasicFunctions;
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

        String salt=BasicFunctions.generateSalt();
        String hash=BasicFunctions.hashPassword("111111", salt);
        userPasswordMapper.updateById(new UserPassword(11L, hash, salt, "teacher"));
    }

    /**一次性迁移：把 userPassword 表中的明文密码全部转换为“每用户随机盐 + PBKDF2 加盐哈希”。
     * 转换后原有密码不变（明文是什么，哈希对应的就是什么），无需重新设置密码。
     * 已迁移的行（salt 非空且密码为 64 位十六进制）自动跳过，可重复执行。*/
    @Test
    public void testMigratePasswordsToHash(){
        List<UserPassword> userPasswordList=userPasswordMapper.selectList(null);
        int migrated=0;
        for (UserPassword userPassword : userPasswordList) {
            String password=userPassword.getPassword();
            if (userPassword.getSalt()!=null && password!=null && password.matches("[0-9a-f]{64}")) {
                continue; // 已迁移
            }
            String plain = password==null ? "" : password;
            String salt=BasicFunctions.generateSalt();
            String hash=BasicFunctions.hashPassword(plain, salt);
            userPasswordMapper.updateById(new UserPassword(userPassword.getId(), hash, salt, userPassword.getPosition()));
            migrated++;
        }
        System.out.println("==============已迁移 " + migrated + " 个用户的密码=================");
        Assert.isTrue(migrated <= userPasswordList.size(), "");
    }

    @Test
    public void testCreateUser(){
        User user;
        UserPassword userPassword;
        Long id=12L;

        user=new User(id, "Hherern", 20, "hherern@163.com");
        String salt=BasicFunctions.generateSalt();
        userPassword=new UserPassword(id, BasicFunctions.hashPassword("mySystem", salt), salt, "admin");

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
            userTaskMapper.insert(new UserTask("修改密码", "all", "", "/change_password", LocalDateTime.now(), localDateTime));
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
    public void testSelectSelectedCourses(){
        // 与 testCourseSelect 类似的准备逻辑：没有选课数据时先造几条
        if(courseSelectMapper.selectList(null).isEmpty()) {
            courseSelectMapper.insert(new CourseSelect(1L, 2L, LocalDateTime.now(), YearTerm.now().toString()));
            courseSelectMapper.insert(new CourseSelect(1L, 3L, LocalDateTime.now(), YearTerm.now().toString()));
            courseSelectMapper.insert(new CourseSelect(1L, 4L, LocalDateTime.now(), YearTerm.now().toString()));
        }

        String currentTerm = YearTerm.now().toString();
        System.out.println("==============当前学期：" + currentTerm + "=================");

        for (Long userId = 1L; userId <= 4L; userId++) {
            List<SelectedCourseView> views = courseSelectMapper.selectSelectedCourses(userId, currentTerm);
            System.out.println("==============" + userId + "号学生选的课程：=================");
            views.forEach(System.out::println);
        }
    }

    @Test
    public void testSelectCoursesToSelect(){
        String nextTerm = YearTerm.now().next().toString();
        System.out.println("==============下一学期：" + nextTerm + "=================");

        // 准备逻辑：下一学期还没有课程时先造几条（含一门可能不开课的）
        Map<String, Object> termMap=new HashMap<>();
        termMap.put("term", nextTerm);
        if(courseMapper.selectByMap(termMap).size()<=3) {
            courseMapper.insert(new Course(11L, "编译原理", nextTerm, "专业必修课", true, 4));
            courseMapper.insert(new Course(12L, "操作系统", nextTerm, "专业必修课", true, 4));
            courseMapper.insert(new Course(13L, "机器学习", nextTerm, "专业选修课", false, 3));
        }

        // 取下一学期的一门课，让1号学生选它（重复执行测试时避免冲突，先删再插）
        List<Course> nextTermCourses = courseMapper.selectByMap(termMap);
        Long courseId = nextTermCourses.get(0).getId();
        Map<String, Object> deleteMap=new HashMap<>();
        deleteMap.put("user_id", 1L);
        deleteMap.put("course_id", courseId);
        courseSelectMapper.deleteByMap(deleteMap);
        courseSelectMapper.insert(new CourseSelect(1L, courseId, LocalDateTime.now(), YearTerm.now().toString()));

        System.out.println("==============不筛选是否开课=================");
        for (Long userId = 1L; userId <= 2L; userId++) {
            List<SelectingCourseView> views = courseSelectMapper.selectCoursesToSelect(userId, nextTerm, false);
            System.out.println("==============" + userId + "号学生看到的待选课程：=================");
            views.forEach(System.out::println);
        }

        System.out.println("==============只显示确定开课的=================");
        List<SelectingCourseView> views = courseSelectMapper.selectCoursesToSelect(1L, nextTerm, true);
        views.forEach(System.out::println);
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





