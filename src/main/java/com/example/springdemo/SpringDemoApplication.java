package com.example.springdemo;

import com.example.springdemo.entity.UserPosition;
import com.example.springdemo.mapper.UserMapper;
import com.example.springdemo.manager.LoginCounter;
import jakarta.annotation.Resource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.*;


@SpringBootApplication
@EnableScheduling
@RestController//适合前后端分离的项目
@RequestMapping("/index")
@MapperScan("com.example.springdemo.mapper")
public class SpringDemoApplication {

    @Resource
    private UserMapper userMapper;

    @Resource
    private LoginCounter loginCounter;

    public static void main(String[] args) {
        SpringApplication.run(SpringDemoApplication.class, args);
    }

    @RequestMapping
    public String index(){
        return "Hello, world!";
    }

//    @RequestMapping("/test")
//    public String test(){
//        StringBuilder result;
//        System.out.println(("----- selectAll method test ------"));
//        List<User> userList = userMapper.selectList(null);
//        userList.forEach(System.out::println);
//        if(userList.size()==5){
//            result = new StringBuilder("正确");
//        }
//        else {
//            result = new StringBuilder("错误，有" + userList.size() + "个元素");
//        }
//        result.append("<br>");
//        for (User user:userList){
//            result.append(user).append("<br>");
//        }
//        return result.toString();
//    }

    @GetMapping("/get")
    public String f2(){
        return "Get api without params.\nteacher: "+ loginCounter.getCount(UserPosition.TEACHER)
                +"; student: "+ loginCounter.getCount(UserPosition.STUDENT);
    }

    @GetMapping("/get/{id}")
    public String f3(@PathVariable Long id){
        return "(Get restful请求传值方法)id is "+id;
    }
    @GetMapping("/get3")
    public String f4(@RequestParam Long id, @RequestParam String name){
        return "(Get ) id is : " + id + ", name is : " + name;
    }

}
