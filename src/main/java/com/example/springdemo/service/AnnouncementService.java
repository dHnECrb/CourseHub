package com.example.springdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springdemo.dto.AnnouncementView;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.entity.Announcement;
import com.example.springdemo.entity.AnnouncementRead;
import com.example.springdemo.entity.User;
import com.example.springdemo.entity.UserPosition;
import com.example.springdemo.mapper.AnnouncementMapper;
import com.example.springdemo.mapper.AnnouncementReadMapper;
import com.example.springdemo.mapper.UserMapper;
import com.example.springdemo.util.BasicFunctions;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {
    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_CONTENT_LENGTH = 5000;

    @Resource
    private MessageSource msgSource;

    @Resource
    private AnnouncementMapper announcementMapper;

    @Resource
    private AnnouncementReadMapper announcementReadMapper;

    @Resource
    private UserMapper userMapper;

    private boolean isAdmin(@NonNull HttpSession httpSession) {
        Object position = httpSession.getAttribute("userPosition");
        return httpSession.getAttribute("userId") != null
                && position instanceof UserPosition && position == UserPosition.ADMIN;
    }

    /**管理员进入公告管理页面。*/
    public RequestResult index(@NonNull HttpSession httpSession) {
        Locale locale = Locale.CHINA;
        if (httpSession.getAttribute("userId") == null) {
            return new RequestResult(false, 401,
                    msgSource.getMessage("responseMessage.didNotLogin", null, locale));
        }
        if (!isAdmin(httpSession)) {
            return new RequestResult(false, 403,
                    msgSource.getMessage("responseMessage.accessDenied", null, locale));
        }
        return new RequestResult(true, 200, null);
    }

    public Announcement getLatestAnnouncement() {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Announcement::getPublish_time)
                .orderByDesc(Announcement::getId)
                .last("LIMIT 1");
        return announcementMapper.selectOne(wrapper);
    }

    /**取得进入主界面时需要弹出的公告；已选择不再提示时返回 null。*/
    public Announcement getAnnouncementToDisplay(@NonNull HttpSession httpSession) {
        if (httpSession.getAttribute("userId") == null) {
            return null;
        }
        Announcement announcement = getLatestAnnouncement();
        if (announcement == null || hasRead(httpSession, announcement.getId())) {
            return null;
        }
        return announcement;
    }

    /**查询全部历史公告并批量补充发布者姓名，不改变用户的提示设置。*/
    public List<AnnouncementView> getAllAnnouncements() {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Announcement::getPublish_time)
                .orderByDesc(Announcement::getId);
        List<Announcement> announcements = announcementMapper.selectList(wrapper);
        if (announcements.isEmpty()) {
            return List.of();
        }

        List<Long> publisherIds = announcements.stream()
                .map(Announcement::getPublisher_id)
                .distinct()
                .toList();
        Map<Long, String> nameById = userMapper.selectBatchIds(publisherIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
        return announcements.stream()
                .map(announcement -> new AnnouncementView(
                        announcement.getTitle(),
                        announcement.getContent(),
                        nameById.getOrDefault(announcement.getPublisher_id(), "未知用户"),
                        announcement.getPublish_time()))
                .toList();
    }

    /**发布一条新公告，历史公告及用户的不再提示记录均保留。*/
    public RequestResult publish(@NonNull HttpSession httpSession, String title, String content) {
        Locale locale = Locale.CHINA;
        RequestResult permission = index(httpSession);
        if (!permission.isSucceed()) {
            return permission;
        }
        String actualTitle = title == null ? "" : title.trim();
        String actualContent = content == null ? "" : content.trim();
        if (actualTitle.isEmpty() || actualContent.isEmpty()) {
            return new RequestResult(false, -1,
                    msgSource.getMessage("responseMessage.announcementEmpty", null, locale));
        }
        if (actualTitle.length() > MAX_TITLE_LENGTH || actualContent.length() > MAX_CONTENT_LENGTH) {
            return new RequestResult(false, -1,
                    msgSource.getMessage("responseMessage.announcementTooLong", null, locale));
        }

        Long publisherId = Long.valueOf((String) httpSession.getAttribute("userId"));
        Announcement announcement = new Announcement(actualTitle, actualContent,
                publisherId, LocalDateTime.now());
        announcementMapper.insert(announcement);
        BasicFunctions.printTimeHttpSessionAnd(httpSession, publisherId.toString(),
                msgSource.getMessage("logMessage.publishAnnouncement",
                        new Object[]{announcement.getId().toString()}, locale));
        return new RequestResult(true, 200,
                msgSource.getMessage("responseMessage.announcementPublished", null, locale));
    }

    /**保存用户对指定公告的“不再提示”设置，重复请求按成功处理。*/
    public RequestResult markAsRead(@NonNull HttpSession httpSession, Long announcementId) {
        Locale locale = Locale.CHINA;
        if (httpSession.getAttribute("userId") == null) {
            return new RequestResult(false, 401,
                    msgSource.getMessage("responseMessage.didNotLogin", null, locale));
        }
        if (announcementId == null || announcementMapper.selectById(announcementId) == null) {
            return new RequestResult(false, 404,
                    msgSource.getMessage("responseMessage.announcementNotExists", null, locale));
        }
        if (!hasRead(httpSession, announcementId)) {
            Long userId = Long.valueOf((String) httpSession.getAttribute("userId"));
            try {
                announcementReadMapper.insert(new AnnouncementRead(
                        announcementId, userId, LocalDateTime.now()));
            } catch (DuplicateKeyException ignored) {
                // 并发重复提交已经产生相同记录，最终状态符合请求目的。
            }
            BasicFunctions.printTimeHttpSessionAnd(httpSession, userId.toString(),
                    msgSource.getMessage("logMessage.readAnnouncement",
                            new Object[]{announcementId.toString()}, locale));
        }
        return new RequestResult(true, 200,
                msgSource.getMessage("responseMessage.announcementRead", null, locale));
    }

    private boolean hasRead(@NonNull HttpSession httpSession, @NonNull Long announcementId) {
        Long userId = Long.valueOf((String) httpSession.getAttribute("userId"));
        LambdaQueryWrapper<AnnouncementRead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnnouncementRead::getAnnouncement_id, announcementId)
                .eq(AnnouncementRead::getUser_id, userId);
        return announcementReadMapper.selectCount(wrapper) > 0;
    }
}
