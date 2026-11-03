package com.example.springdemo.service;

import com.example.springdemo.dto.AnnouncementView;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.entity.Announcement;
import com.example.springdemo.entity.AnnouncementRead;
import com.example.springdemo.entity.User;
import com.example.springdemo.entity.UserPosition;
import com.example.springdemo.mapper.AnnouncementMapper;
import com.example.springdemo.mapper.AnnouncementReadMapper;
import com.example.springdemo.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTests {

    @Mock
    private MessageSource msgSource;

    @Mock
    private AnnouncementMapper announcementMapper;

    @Mock
    private AnnouncementReadMapper announcementReadMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private AnnouncementService announcementService;

    @BeforeEach
    void setUp() {
        lenient().when(httpSession.getAttribute("userId")).thenReturn("12");
        lenient().when(httpSession.getAttribute("userPosition")).thenReturn(UserPosition.ADMIN);
        lenient().when(msgSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rejectsNotLoggedInAdminPage() {
        when(httpSession.getAttribute("userId")).thenReturn(null);

        RequestResult result = announcementService.index(httpSession);

        assertFalse(result.isSucceed());
        assertEquals(401, result.code());
    }

    @Test
    void rejectsNonAdminPublish() {
        when(httpSession.getAttribute("userPosition")).thenReturn(UserPosition.STUDENT);

        RequestResult result = announcementService.publish(httpSession, "标题", "正文");

        assertFalse(result.isSucceed());
        assertEquals(403, result.code());
        verifyNoInteractions(announcementMapper, announcementReadMapper);
    }

    @Test
    void rejectsEmptyAnnouncement() {
        RequestResult result = announcementService.publish(httpSession, " ", "正文");

        assertFalse(result.isSucceed());
        verifyNoInteractions(announcementMapper, announcementReadMapper);
    }

    @Test
    void rejectsTooLongAnnouncement() {
        String longContent = "公".repeat(AnnouncementService.MAX_CONTENT_LENGTH + 1);

        RequestResult result = announcementService.publish(httpSession, "标题", longContent);

        assertFalse(result.isSucceed());
        verifyNoInteractions(announcementMapper, announcementReadMapper);
    }

    @Test
    void publishesTrimmedAnnouncement() {
        doAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);
            announcement.setId(8L);
            return 1;
        }).when(announcementMapper).insert(any(Announcement.class));

        RequestResult result = announcementService.publish(httpSession, "  标题  ", "  正文  ");

        assertTrue(result.isSucceed());
        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementMapper).insert(captor.capture());
        assertEquals("标题", captor.getValue().getTitle());
        assertEquals("正文", captor.getValue().getContent());
        assertEquals(12L, captor.getValue().getPublisher_id());
    }

    @Test
    void returnsNullWhenNoAnnouncementExists() {
        when(announcementMapper.selectOne(any())).thenReturn(null);

        assertNull(announcementService.getAnnouncementToDisplay(httpSession));
        verifyNoInteractions(announcementReadMapper);
    }

    @Test
    void returnsLatestAnnouncementWhenUserHasNotDismissedIt() {
        Announcement announcement = announcement(8L);
        when(announcementMapper.selectOne(any())).thenReturn(announcement);
        when(announcementReadMapper.selectCount(any())).thenReturn(0L);

        assertSame(announcement, announcementService.getAnnouncementToDisplay(httpSession));
    }

    @Test
    void hidesAnnouncementAfterUserChoosesNotToRemind() {
        when(announcementMapper.selectOne(any())).thenReturn(announcement(8L));
        when(announcementReadMapper.selectCount(any())).thenReturn(1L);

        assertNull(announcementService.getAnnouncementToDisplay(httpSession));
    }

    @Test
    void showsNewAnnouncementEvenWhenOldAnnouncementWasDismissed() {
        Announcement newAnnouncement = announcement(9L);
        when(announcementMapper.selectOne(any())).thenReturn(newAnnouncement);
        when(announcementReadMapper.selectCount(any())).thenReturn(0L);

        assertSame(newAnnouncement, announcementService.getAnnouncementToDisplay(httpSession));
    }

    @Test
    void returnsAllAnnouncementsWithPublisherNames() {
        List<Announcement> announcements = List.of(
                announcement(9L),
                new Announcement(8L, "标题", "正文", 2L, LocalDateTime.of(2026, 11, 1, 10, 0)),
                announcement(7L));
        when(announcementMapper.selectList(any())).thenReturn(announcements);
        when(userMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(
                new User(2L, "管理员乙", 30, "b@example.com"),
                new User(1L, "管理员甲", 30, "a@example.com")));

        List<AnnouncementView> views = announcementService.getAllAnnouncements();

        assertEquals(3, views.size());
        assertEquals("管理员甲", views.get(0).getPublisher_name());
        assertEquals("管理员乙", views.get(1).getPublisher_name());
        assertEquals("管理员甲", views.get(2).getPublisher_name());
        verifyNoInteractions(announcementReadMapper);
    }

    @Test
    void showsUnknownPublisherWhenUserDoesNotExist() {
        when(announcementMapper.selectList(any())).thenReturn(List.of(announcement(8L)));
        when(userMapper.selectBatchIds(List.of(1L))).thenReturn(List.of());

        List<AnnouncementView> views = announcementService.getAllAnnouncements();

        assertEquals("未知用户", views.get(0).getPublisher_name());
    }

    @Test
    void doesNotQueryUsersWhenNoAnnouncementExists() {
        when(announcementMapper.selectList(any())).thenReturn(List.of());

        assertTrue(announcementService.getAllAnnouncements().isEmpty());
        verifyNoInteractions(userMapper, announcementReadMapper);
    }

    @Test
    void rejectsMarkingMissingAnnouncementAsRead() {
        when(announcementMapper.selectById(8L)).thenReturn(null);

        RequestResult result = announcementService.markAsRead(httpSession, 8L);

        assertFalse(result.isSucceed());
        assertEquals(404, result.code());
        verifyNoInteractions(announcementReadMapper);
    }

    @Test
    void savesDismissalOnce() {
        when(announcementMapper.selectById(8L)).thenReturn(announcement(8L));
        when(announcementReadMapper.selectCount(any())).thenReturn(0L);

        RequestResult result = announcementService.markAsRead(httpSession, 8L);

        assertTrue(result.isSucceed());
        verify(announcementReadMapper).insert(any(AnnouncementRead.class));
    }

    @Test
    void repeatedDismissalDoesNotInsertAgain() {
        when(announcementMapper.selectById(8L)).thenReturn(announcement(8L));
        when(announcementReadMapper.selectCount(any())).thenReturn(1L);

        RequestResult result = announcementService.markAsRead(httpSession, 8L);

        assertTrue(result.isSucceed());
        verify(announcementReadMapper, never()).insert(any(AnnouncementRead.class));
    }

    @Test
    void concurrentDuplicateDismissalIsSuccessful() {
        when(announcementMapper.selectById(8L)).thenReturn(announcement(8L));
        when(announcementReadMapper.selectCount(any())).thenReturn(0L);
        when(announcementReadMapper.insert(any(AnnouncementRead.class)))
                .thenThrow(new DuplicateKeyException("重复"));

        RequestResult result = announcementService.markAsRead(httpSession, 8L);

        assertTrue(result.isSucceed());
    }

    private static Announcement announcement(Long id) {
        return new Announcement(id, "标题", "正文", 1L, LocalDateTime.of(2026, 11, 2, 10, 0));
    }
}
