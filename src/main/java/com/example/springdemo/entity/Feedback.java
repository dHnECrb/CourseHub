package com.example.springdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`feedback`")
public class Feedback {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long user_id;
    private String content;
    private LocalDateTime submit_time;
    private Boolean is_read;
    private LocalDateTime read_time;
    private Boolean is_deleted;
    private LocalDateTime delete_time;

    public Feedback(Long id, Long user_id, String content, LocalDateTime submit_time,
                    Boolean is_read, LocalDateTime read_time, Boolean is_deleted, LocalDateTime delete_time) {
        this.id = id;
        this.user_id = user_id;
        this.content = content;
        this.submit_time = submit_time;
        this.is_read = is_read;
        this.read_time = read_time;
        this.is_deleted = is_deleted;
        this.delete_time = delete_time;
    }

    public Feedback(Long user_id, String content, LocalDateTime submit_time) {
        this(null, user_id, content, submit_time, false, null, false, null);
    }

    public Long getId() {
        return id;
    }

    public Long getUser_id() {
        return user_id;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getSubmit_time() {
        return submit_time;
    }

    public Boolean getIs_read() {
        return is_read;
    }

    public LocalDateTime getRead_time() {
        return read_time;
    }

    public Boolean getIs_deleted() {
        return is_deleted;
    }

    public LocalDateTime getDelete_time() {
        return delete_time;
    }
}
