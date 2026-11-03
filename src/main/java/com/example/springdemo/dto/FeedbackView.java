package com.example.springdemo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class FeedbackView {
    private Long id;
    private Long user_id;
    private String user_name;
    private String content;
    private LocalDateTime submit_time;
    private Boolean is_read;
    private LocalDateTime read_time;
    private Boolean is_deleted;
    private LocalDateTime delete_time;

    public FeedbackView(Long id, Long user_id, String user_name, String content, LocalDateTime submit_time,
                        Boolean is_read, LocalDateTime read_time, Boolean is_deleted, LocalDateTime delete_time) {
        this.id = id;
        this.user_id = user_id;
        this.user_name = user_name;
        this.content = content;
        this.submit_time = submit_time;
        this.is_read = is_read;
        this.read_time = read_time;
        this.is_deleted = is_deleted;
        this.delete_time = delete_time;
    }
}
