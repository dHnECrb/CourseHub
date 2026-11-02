package com.example.springdemo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`announcement_read`")
public class AnnouncementRead {
    private Long announcement_id;
    private Long user_id;
    private LocalDateTime read_time;

    public AnnouncementRead(Long announcement_id, Long user_id, LocalDateTime read_time) {
        this.announcement_id = announcement_id;
        this.user_id = user_id;
        this.read_time = read_time;
    }

    public Long getAnnouncement_id() {
        return announcement_id;
    }

    public Long getUser_id() {
        return user_id;
    }

    public LocalDateTime getRead_time() {
        return read_time;
    }
}
