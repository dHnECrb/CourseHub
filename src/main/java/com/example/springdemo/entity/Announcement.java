package com.example.springdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`announcement`")
public class Announcement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private Long publisher_id;
    private LocalDateTime publish_time;

    public Announcement(Long id, String title, String content, Long publisher_id, LocalDateTime publish_time) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.publisher_id = publisher_id;
        this.publish_time = publish_time;
    }

    public Announcement(String title, String content, Long publisher_id, LocalDateTime publish_time) {
        this(null, title, content, publisher_id, publish_time);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Long getPublisher_id() {
        return publisher_id;
    }

    public LocalDateTime getPublish_time() {
        return publish_time;
    }
}
