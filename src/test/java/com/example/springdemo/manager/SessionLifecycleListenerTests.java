package com.example.springdemo.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionExpiredEvent;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionLifecycleListenerTests {

    @Mock
    private OnlineUserManager onlineUserManager;

    @Mock
    private SessionDeletedEvent deletedEvent;

    @Mock
    private SessionExpiredEvent expiredEvent;

    @InjectMocks
    private SessionLifecycleListener sessionLifecycleListener;

    @Test
    void deletedSessionRemovesOnlineState() {
        when(deletedEvent.getSessionId()).thenReturn("session-1");

        sessionLifecycleListener.onSessionDeleted(deletedEvent);

        verify(onlineUserManager).logoutById("session-1");
    }

    @Test
    void expiredSessionRemovesOnlineState() {
        when(expiredEvent.getSessionId()).thenReturn("session-2");

        sessionLifecycleListener.onSessionExpired(expiredEvent);

        verify(onlineUserManager).logoutById("session-2");
    }
}
