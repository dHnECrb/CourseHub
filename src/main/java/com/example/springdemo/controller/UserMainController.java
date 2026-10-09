package com.example.springdemo.controller;


import com.example.springdemo.dto.HeartBeatRequest;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.entity.UserTask;
import com.example.springdemo.service.UserMainService;
import com.example.springdemo.service.UserTaskService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;


@RestController
@RequestMapping("/main")
public class UserMainController {
//    @Resource
//    private UserLoginService userLoginService;

    @Resource
    private UserMainService userMainService;

    @Resource
    private UserTaskService userTaskService;

    @Resource
    private MessageSource msgSource;

//    public UserMainController(UserLoginService userLoginService) {
//        this.userLoginService=userLoginService;
//    }


    @GetMapping
    public ModelAndView index(@NonNull HttpSession httpSession) {// 通过localhost:port/app_1/main访问
//        List<UserTask> userTaskList;
        ModelAndView modelAndView = new ModelAndView();
        boolean willGoUserMain=userMainService.canGoUserMain(httpSession);// 判断是否已经登录

        if (!willGoUserMain){
            modelAndView.setViewName("login_need");
            return modelAndView;
        }

//        userTaskList=userTaskService.getActiveTaskByPosition(httpSession);
        modelAndView.setViewName("user_main");
        modelAndView.addObject("userId", httpSession.getAttribute("userId"));
//        modelAndView.addObject("tasks",userTaskList);

        return modelAndView;
    }


//    @PostMapping("/heartbeat")
//    public ResponseEntity<Void> heartbeat(@NonNull HttpSession httpSession){
//        boolean result= userMainService.heartbeat(httpSession);
//        if (result) {
//            return ResponseEntity.ok().build(); // 200 ok
//        }
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401错误
//    }

    @PostMapping("/heartbeat")
    public boolean heartbeat(@NonNull HttpSession httpSession, @NonNull HttpServletResponse response,
                             @RequestBody HeartBeatRequest lastActiveTime) throws IOException {
        RequestResult requestResult = userMainService.heartbeat(httpSession, lastActiveTime.getLastActiveTime().getTime());
        if(requestResult.isSucceed()){
            return true;
        }
        if (requestResult.code() == 200) {
            return false;
        }
        if(requestResult.code()==401){
            response.sendError(401);
            return false;
        }
        throw new IllegalArgumentException("无效代码");
    }

    @GetMapping("/tasks")
    public List<UserTask> getTasks(@NonNull HttpSession httpSession,
                                   @RequestParam(defaultValue = "false") boolean filterTime) {
        // 根据 filterTime 做筛选查询
        if(filterTime) {
            return userTaskService.getActiveTaskByPosition(httpSession);
        }
        else{
            return userTaskService.getTaskByPosition(httpSession);
        }
    }

    @GetMapping("/logout")
    public ModelAndView logout(@NonNull HttpSession httpSession){
        ModelAndView modelAndView = new ModelAndView();
        userMainService.logoutManually(httpSession);
        modelAndView.setViewName("redirect:/login");
        return modelAndView;
    }

    @GetMapping("/logoutAuto")
    public ModelAndView logoutDueToNoAction(@NonNull HttpSession httpSession){
        ModelAndView modelAndView = new ModelAndView();
        Locale locale=Locale.CHINA;
        userMainService.logoutDueToNoAction(httpSession);
        modelAndView.setViewName("login_info_general");
        modelAndView.addObject("title", "自动退出");
//        modelAndView.addObject("parent", "../");
        modelAndView.addObject("h1_text","自动退出");
        modelAndView.addObject("reason",msgSource.getMessage("responseMessage.logoutDueToNoAction", null, locale));
        modelAndView.addObject("button_event","/app_1/login");
        modelAndView.addObject("button_text","登    录");
        return modelAndView;
    }
}
