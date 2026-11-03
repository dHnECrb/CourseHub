package com.example.springdemo.manager;

import jakarta.annotation.Resource;
import org.springframework.context.event.EventListener;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.stereotype.Component;

@Component
public class SessionLifecycleListener {

    @Resource
    private OnlineUserManager onlineUserManager;

    @EventListener
    public void onSessionDeleted(SessionDeletedEvent event) {
        onlineUserManager.logoutById(event.getSessionId());
    }

    @EventListener
    public void onSessionExpired(SessionExpiredEvent event) {
        onlineUserManager.logoutById(event.getSessionId());
    }
}
