package com.example.springdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.springdemo.dto.FeedbackFilter;
import com.example.springdemo.dto.FeedbackView;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.entity.Feedback;
import com.example.springdemo.entity.User;
import com.example.springdemo.mapper.FeedbackMapper;
import com.example.springdemo.mapper.UserMapper;
import com.example.springdemo.util.BasicFunctions;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FeedbackService {
    public static final int MAX_CONTENT_LENGTH = 800;
    public static final int MAX_DAILY_FEEDBACK = 1;
    public static final String USER_SITE = "/feedback";
    public static final String ADMIN_SITE = "/admin/feedback_manage";
    private static final DateTimeFormatter CSV_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private FeedbackMapper feedbackMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserTaskService userTaskService;

    @Resource
    private MessageSource msgSource;

    public RequestResult index(@NonNull HttpSession httpSession) {
        return userTaskService.goTask(httpSession, "意见反馈", USER_SITE, Locale.CHINA);
    }

    public RequestResult indexAdmin(@NonNull HttpSession httpSession) {
        return userTaskService.goTask(httpSession, "意见反馈管理", ADMIN_SITE, Locale.CHINA);
    }

    /**查询当前用户未删除的意见，已删除记录仍保留在数据库中。*/
    public List<Feedback> getUserFeedback(@NonNull String userId) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Feedback::getUser_id, Long.valueOf(userId))
                .eq(Feedback::getIs_deleted, false)
                .orderByDesc(Feedback::getSubmit_time)
                .orderByDesc(Feedback::getId);
        return feedbackMapper.selectList(wrapper);
    }

    /**提交意见，每日限制统计包含当天已经软删除的意见。*/
    public RequestResult submit(@NonNull HttpSession httpSession, String content) {
        Locale locale = Locale.CHINA;
        RequestResult permission = index(httpSession);
        if (!permission.isSucceed()) {
            return permission;
        }
        String actualContent = content == null ? "" : content.trim();
        if (actualContent.isEmpty()) {
            return new RequestResult(false, -1,
                    msgSource.getMessage("responseMessage.feedbackEmpty", null, locale));
        }
        if (actualContent.length() > MAX_CONTENT_LENGTH) {
            return new RequestResult(false, -1,
                    msgSource.getMessage("responseMessage.feedbackTooLong", null, locale));
        }

        String userId = (String) httpSession.getAttribute("userId");
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LambdaQueryWrapper<Feedback> dailyWrapper = new LambdaQueryWrapper<>();
        dailyWrapper.eq(Feedback::getUser_id, Long.valueOf(userId))
                .ge(Feedback::getSubmit_time, today.atStartOfDay())
                .lt(Feedback::getSubmit_time, today.plusDays(1).atStartOfDay());
        if (feedbackMapper.selectCount(dailyWrapper) >= MAX_DAILY_FEEDBACK) {
            return new RequestResult(false, -1,
                    msgSource.getMessage("responseMessage.feedbackDailyLimit", null, locale));
        }

        Feedback feedback = new Feedback(Long.valueOf(userId), actualContent, now);
        feedbackMapper.insert(feedback);
        BasicFunctions.printTimeHttpSessionAnd(httpSession, userId,
                msgSource.getMessage("logMessage.submitFeedback",
                        new Object[]{feedback.getId().toString()}, locale));
        return new RequestResult(true, 200,
                msgSource.getMessage("responseMessage.feedbackSubmitted", null, locale));
    }

    /**仅意见所有者可以软删除自己的未删除意见。*/
    public RequestResult delete(@NonNull HttpSession httpSession, Long feedbackId) {
        Locale locale = Locale.CHINA;
        RequestResult permission = index(httpSession);
        if (!permission.isSucceed()) {
            return permission;
        }
        String userId = (String) httpSession.getAttribute("userId");
        Feedback feedback = feedbackId == null ? null : feedbackMapper.selectById(feedbackId);
        if (feedback == null || !feedback.getUser_id().equals(Long.valueOf(userId))
                || Boolean.TRUE.equals(feedback.getIs_deleted())) {
            return new RequestResult(false, 404,
                    msgSource.getMessage("responseMessage.feedbackNotExists", null, locale));
        }

        LambdaUpdateWrapper<Feedback> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Feedback::getId, feedbackId)
                .eq(Feedback::getUser_id, Long.valueOf(userId))
                .eq(Feedback::getIs_deleted, false)
                .set(Feedback::getIs_deleted, true)
                .set(Feedback::getDelete_time, LocalDateTime.now());
        feedbackMapper.update(null, wrapper);
        BasicFunctions.printTimeHttpSessionAnd(httpSession, userId,
                msgSource.getMessage("logMessage.deleteFeedback",
                        new Object[]{feedbackId.toString()}, locale));
        return new RequestResult(true, 200,
                msgSource.getMessage("responseMessage.feedbackDeleted", null, locale));
    }

    /**按管理页面的组合条件查询，并批量补充用户姓名。*/
    public List<FeedbackView> getFeedback(FeedbackFilter filter) {
        FeedbackFilter actualFilter = filter == null ? FeedbackFilter.defaultFilter() : filter;
        LambdaQueryWrapper<Feedback> wrapper = buildFilterWrapper(actualFilter);
        List<Feedback> feedbackList = feedbackMapper.selectList(wrapper);
        if (feedbackList.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = feedbackList.stream()
                .map(Feedback::getUser_id)
                .distinct()
                .toList();
        Map<Long, String> nameById = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
        return feedbackList.stream()
                .map(feedback -> new FeedbackView(feedback.getId(), feedback.getUser_id(),
                        nameById.getOrDefault(feedback.getUser_id(), "未知用户"), feedback.getContent(),
                        feedback.getSubmit_time(), feedback.getIs_read(), feedback.getRead_time(),
                        feedback.getIs_deleted(), feedback.getDelete_time()))
                .toList();
    }

    /**切换管理员已读状态，未读状态不保留已读时间。*/
    public RequestResult markRead(@NonNull HttpSession httpSession, Long feedbackId, boolean read) {
        Locale locale = Locale.CHINA;
        RequestResult permission = indexAdmin(httpSession);
        if (!permission.isSucceed()) {
            return permission;
        }
        if (feedbackId == null || feedbackMapper.selectById(feedbackId) == null) {
            return new RequestResult(false, 404,
                    msgSource.getMessage("responseMessage.feedbackNotExists", null, locale));
        }

        LambdaUpdateWrapper<Feedback> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Feedback::getId, feedbackId)
                .set(Feedback::getIs_read, read)
                .set(Feedback::getRead_time, read ? LocalDateTime.now() : null);
        feedbackMapper.update(null, wrapper);
        String userId = (String) httpSession.getAttribute("userId");
        BasicFunctions.printTimeHttpSessionAnd(httpSession, userId,
                msgSource.getMessage("logMessage.markFeedbackRead",
                        new Object[]{feedbackId.toString(), read ? "已读" : "未读"}, locale));
        return new RequestResult(true, 200,
                msgSource.getMessage("responseMessage.feedbackReadChanged", null, locale));
    }

    /**按当前筛选条件导出带 BOM 的 CSV，便于直接使用表格软件打开。*/
    public byte[] exportFeedback(FeedbackFilter filter) {
        List<FeedbackView> feedbackList = getFeedback(filter);
        StringBuilder csv = new StringBuilder();
        appendCsvRow(csv, "意见编号", "用户ID", "用户姓名", "提交时间", "是否已读", "已读时间",
                "是否删除", "删除时间", "反馈正文");
        for (FeedbackView feedback : feedbackList) {
            appendCsvRow(csv, feedback.getId(), feedback.getUser_id(), feedback.getUser_name(),
                    formatTime(feedback.getSubmit_time()), Boolean.TRUE.equals(feedback.getIs_read()) ? "已读" : "未读",
                    formatTime(feedback.getRead_time()), Boolean.TRUE.equals(feedback.getIs_deleted()) ? "已删除" : "未删除",
                    formatTime(feedback.getDelete_time()), feedback.getContent());
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.writeBytes(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        outputStream.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return outputStream.toByteArray();
    }

    private LambdaQueryWrapper<Feedback> buildFilterWrapper(FeedbackFilter filter) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(filter.startDate() != null, Feedback::getSubmit_time,
                        filter.startDate() == null ? null : filter.startDate().atStartOfDay())
                .lt(filter.endDate() != null, Feedback::getSubmit_time,
                        filter.endDate() == null ? null : filter.endDate().plusDays(1).atStartOfDay())
                .eq(filter.isRead() != null, Feedback::getIs_read, filter.isRead())
                .eq(filter.isDeleted() != null, Feedback::getIs_deleted, filter.isDeleted())
                .orderByDesc(Feedback::getSubmit_time)
                .orderByDesc(Feedback::getId);
        return wrapper;
    }

    private static String formatTime(LocalDateTime time) {
        return time == null ? "" : CSV_TIME_FORMATTER.format(time);
    }

    private static void appendCsvRow(StringBuilder csv, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(csvCell(values[i]));
        }
        csv.append("\r\n");
    }

    private static String csvCell(Object value) {
        String text = value == null ? "" : value.toString();
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
