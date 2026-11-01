package com.example.springdemo.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`task`")
public class UserTask {
    private Long id;                        // 编号
    private String name;                    // 任务名
    private String comment;                 // 任务详细信息
    private String site;                    // 任务入口地址
    private LocalDateTime start_time;       // 任务开始日期
    private LocalDateTime deadline_time;    // 任务截至日期
    private String position;                // 任务属于哪个职位的用户

    public UserTask(Long id, String name, String comment, String site, LocalDateTime start_time, LocalDateTime deadline_time, String position) {
        this.id = id;
        this.name = name;
        this.comment = comment;
        this.site = site;
        this.start_time = start_time;
        this.deadline_time = deadline_time;
        this.position = position;
    }

    public UserTask(String name, String position, String comment, String site, LocalDateTime start_time, LocalDateTime deadline_time) {
        this.name = name;
        this.position = position;
        this.comment = comment;
        this.site = site;
        this.start_time = start_time;
        this.deadline_time = deadline_time;
    }

    public boolean belongTo(String position){
        if(position.equals("all")){
            return true;
        }
        if(this.position.equals("all")){
            return true;
        }
        return position.equals(this.position);
    }

    public boolean belongTo(UserPosition userPosition){
        return this.belongTo(userPosition.getString());
    }

    public UserTask(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public String getSite() {
        return site;
    }

    public LocalDateTime getStart_time() {
        return start_time;
    }

    public LocalDateTime getDeadline_time() {
        return deadline_time;
    }

    public String getPosition() {
        return position;
    }

}
