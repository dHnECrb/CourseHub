package com.example.springdemo.service;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.springdemo.dto.FeedbackFilter;
import com.example.springdemo.dto.FeedbackView;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.entity.Feedback;
import com.example.springdemo.entity.User;
import com.example.springdemo.mapper.FeedbackMapper;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTests {

    @Mock
    private FeedbackMapper feedbackMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserTaskService userTaskService;

    @Mock
    private MessageSource msgSource;

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private FeedbackService feedbackService;

    @org.junit.jupiter.api.BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
                new MybatisConfiguration(), ""), Feedback.class);
    }

    @BeforeEach
    void setUp() {
        lenient().when(httpSession.getAttribute("userId")).thenReturn("12");
        lenient().when(userTaskService.goTask(any(), anyString(), anyString(), any(Locale.class)))
                .thenReturn(new RequestResult(true, 200, null));
        lenient().when(msgSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void usesMybatisPlusGeneratedId() throws NoSuchFieldException {
        TableId tableId = Feedback.class.getDeclaredField("id").getAnnotation(TableId.class);

        assertEquals(IdType.ASSIGN_ID, tableId.type());
    }

    @Test
    void rejectsSubmitWhenTaskIsUnavailable() {
        when(userTaskService.goTask(any(), anyString(), anyString(), any(Locale.class)))
                .thenReturn(new RequestResult(false, 403, "无权限"));

        RequestResult result = feedbackService.submit(httpSession, "建议");

        assertFalse(result.isSucceed());
        assertEquals(403, result.code());
        verifyNoInteractions(feedbackMapper);
    }

    @Test
    void rejectsEmptyFeedback() {
        RequestResult result = feedbackService.submit(httpSession, "  ");

        assertFalse(result.isSucceed());
        verifyNoInteractions(feedbackMapper);
    }

    @Test
    void rejectsFeedbackLongerThan800Characters() {
        RequestResult result = feedbackService.submit(httpSession,
                "意".repeat(FeedbackService.MAX_CONTENT_LENGTH + 1));

        assertFalse(result.isSucceed());
        verifyNoInteractions(feedbackMapper);
    }

    @Test
    void enforcesDailyLimitWithoutExcludingDeletedRows() {
        when(feedbackMapper.selectCount(any())).thenReturn(1L);

        RequestResult result = feedbackService.submit(httpSession, "今天第二条");

        assertFalse(result.isSucceed());
        ArgumentCaptor<LambdaQueryWrapper<Feedback>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(feedbackMapper).selectCount(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("submit_time"));
        assertFalse(captor.getValue().getSqlSegment().contains("is_deleted"));
        verify(feedbackMapper, never()).insert(any(Feedback.class));
    }

    @Test
    void submitsTrimmedFeedbackWithInitialStates() {
        when(feedbackMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            feedback.setId(99L);
            return 1;
        }).when(feedbackMapper).insert(any(Feedback.class));

        RequestResult result = feedbackService.submit(httpSession, "  新的建议  ");

        assertTrue(result.isSucceed());
        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackMapper).insert(captor.capture());
        assertEquals(12L, captor.getValue().getUser_id());
        assertEquals("新的建议", captor.getValue().getContent());
        assertFalse(captor.getValue().getIs_read());
        assertFalse(captor.getValue().getIs_deleted());
    }

    @Test
    void userHistoryExcludesDeletedFeedbackAndUsesDescendingOrder() {
        when(feedbackMapper.selectList(any())).thenReturn(List.of());

        feedbackService.getUserFeedback("12");

        ArgumentCaptor<LambdaQueryWrapper<Feedback>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(feedbackMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("user_id"));
        assertTrue(sql.contains("is_deleted"));
        assertTrue(sql.contains("ORDER BY submit_time DESC,id DESC"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(false));
    }

    @Test
    void rejectsDeletingAnotherUsersFeedback() {
        when(feedbackMapper.selectById(8L)).thenReturn(feedback(8L, 1L));

        RequestResult result = feedbackService.delete(httpSession, 8L);

        assertFalse(result.isSucceed());
        assertEquals(404, result.code());
        verify(feedbackMapper, never()).update(isNull(), any());
    }

    @Test
    void softDeletesOwnFeedbackWithoutPhysicalDelete() {
        when(feedbackMapper.selectById(8L)).thenReturn(feedback(8L, 12L));

        RequestResult result = feedbackService.delete(httpSession, 8L);

        assertTrue(result.isSucceed());
        ArgumentCaptor<Wrapper<Feedback>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(feedbackMapper).update(isNull(), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("is_deleted"));
        verify(feedbackMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void defaultAdminFilterIsUnreadAndNotDeleted() {
        when(feedbackMapper.selectList(any())).thenReturn(List.of());

        feedbackService.getFeedback(null);

        ArgumentCaptor<LambdaQueryWrapper<Feedback>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(feedbackMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("is_read"));
        assertTrue(sql.contains("is_deleted"));
        assertFalse(sql.contains("submit_time >="));
        assertTrue(sql.contains("ORDER BY submit_time DESC,id DESC"));
    }

    @Test
    void adminFilterCombinesInclusiveDatesAndOptionalStates() {
        when(feedbackMapper.selectList(any())).thenReturn(List.of());
        FeedbackFilter filter = new FeedbackFilter(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 3), true, null);

        feedbackService.getFeedback(filter);

        ArgumentCaptor<LambdaQueryWrapper<Feedback>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(feedbackMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(
                LocalDate.of(2026, 11, 1).atStartOfDay()));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(
                LocalDate.of(2026, 11, 4).atStartOfDay()));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(true));
        assertFalse(sql.contains("is_deleted"));
    }

    @Test
    void adminViewsContainUserNames() {
        when(feedbackMapper.selectList(any())).thenReturn(List.of(feedback(8L, 12L)));
        when(userMapper.selectBatchIds(List.of(12L)))
                .thenReturn(List.of(new User(12L, "用户甲", null, null)));

        List<FeedbackView> views = feedbackService.getFeedback(FeedbackFilter.defaultFilter());

        assertEquals(1, views.size());
        assertEquals("用户甲", views.get(0).getUser_name());
    }

    @Test
    void marksFeedbackReadAndUnread() {
        when(feedbackMapper.selectById(8L)).thenReturn(feedback(8L, 12L));

        assertTrue(feedbackService.markRead(httpSession, 8L, true).isSucceed());
        assertTrue(feedbackService.markRead(httpSession, 8L, false).isSucceed());

        verify(feedbackMapper, org.mockito.Mockito.times(2)).update(isNull(), any());
    }

    @Test
    void rejectsMarkingMissingFeedback() {
        when(feedbackMapper.selectById(8L)).thenReturn(null);

        RequestResult result = feedbackService.markRead(httpSession, 8L, true);

        assertFalse(result.isSucceed());
        assertEquals(404, result.code());
        verify(feedbackMapper, never()).update(isNull(), any());
    }

    @Test
    void exportsUtf8BomAndEscapesCsvAndSpreadsheetFormulas() {
        Feedback feedback = feedback(8L, 12L);
        feedback.setContent("=1+1,\"你好\"\n下一行");
        when(feedbackMapper.selectList(any())).thenReturn(List.of(feedback));
        when(userMapper.selectBatchIds(List.of(12L)))
                .thenReturn(List.of(new User(12L, "用户甲", null, null)));

        byte[] bytes = feedbackService.exportFeedback(FeedbackFilter.defaultFilter());

        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                new byte[]{bytes[0], bytes[1], bytes[2]});
        String csv = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\"意见编号\",\"用户ID\""));
        assertTrue(csv.contains("\"'=1+1,\"\"你好\"\"\n下一行\""));
        assertTrue(csv.endsWith("\r\n"));
    }

    private static Feedback feedback(Long id, Long userId) {
        return new Feedback(id, userId, "反馈正文", LocalDateTime.of(2026, 11, 2, 10, 0),
                false, null, false, null);
    }
}
