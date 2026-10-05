package com.example.springdemo.service;

import com.example.springdemo.manager.OnlineUserManager;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.logging.Logger;

@Service
public class ScheduledTasksService {
    private Logger logger=Logger.getLogger(ScheduledTasksService.class.getName());

    @Resource
    private MessageSource msgSource;

    @Resource
    private OnlineUserManager onlineUserManager;

    /**定时将不活跃的用户会话注销*/
    @Scheduled(initialDelay = 30_000, fixedDelay = 30_000)
    public void evictStaleUsers(){
        int deletedCount;
//        logger.info(msgSource.getMessage("logMessage.startLogoutStaleUsers",null, Locale.CHINA));

        deletedCount=onlineUserManager.evictStaleUsers();

        if(deletedCount==0){
//            logger.info(msgSource.getMessage("logMessage.noStaleUsers",null,Locale.CHINA));
        }
        else {
            logger.info(msgSource.getMessage("logMessage.logoutStaleUsersSuccess", new Object[]{deletedCount}, Locale.CHINA));
        }
    }

}
