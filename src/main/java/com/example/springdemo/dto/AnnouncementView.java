package com.example.springdemo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**用于在全部公告页面展示公告及发布者姓名*/
@Data
@NoArgsConstructor
public class AnnouncementView {
    private String title;
    private String content;
    private String publisher_name;
    private LocalDateTime publish_time;

    public AnnouncementView(String title, String content, String publisher_name, LocalDateTime publish_time) {
        this.title = title;
        this.content = content;
        this.publisher_name = publisher_name;
        this.publish_time = publish_time;
    }
}
