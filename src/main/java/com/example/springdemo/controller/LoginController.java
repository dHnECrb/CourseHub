package com.example.springdemo.controller;

import com.example.springdemo.dto.UserToLogin;
import com.example.springdemo.manager.LoginCounter;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.service.UserLoginService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.Locale;

@RestController
@RequestMapping("/login")
public class LoginController {

//    private int loginCount=0;

    @Resource
    private MessageSource msgSource;

    @Resource
    private UserLoginService userLoginService;

    @Resource
    private LoginCounter loginCounter;

    public LoginController(UserLoginService userLoginService) {
        this.userLoginService = userLoginService;
    }

    @GetMapping
    public ModelAndView index(@NonNull HttpSession httpSession) {// 通过localhost:port/app_1/login访问
        String result;
        if(userLoginService.isAlreadyLoggedIn(httpSession)) {
            result="login_already";
        }
        else {
            result="index";// 对应 templates/index.html
        }
        return new ModelAndView(result);
    }


    @PostMapping
    public ModelAndView login(@NonNull UserToLogin user, @NonNull HttpSession httpSession){
        RequestResult requestResult;
        Locale locale = Locale.CHINA;

        requestResult=userLoginService.login(httpSession, user);
        if(requestResult.isSucceed()) {
            ModelAndView modelAndView = new ModelAndView("login_success");
//            loginCount++;
//            System.out.println("第"+loginCount+"个人次登录");
            modelAndView.addObject("msg",
                    msgSource.getMessage("responseMessage.loginCount",
                            new Object[]{Long.toString(loginCounter.getTotalCount())},
                            locale));
            return modelAndView;
        }
        else {
            ModelAndView modelAndView=new ModelAndView("index");
            if(requestResult.string().equals(
                    msgSource.getMessage(
                            "responseMessage.nameOrPasswordWrong",
                            null,
                            locale))){
                modelAndView.addObject("userId", user.getId());
            }
            modelAndView.addObject("msg",requestResult.string());
            return modelAndView;
        }
    }
}
