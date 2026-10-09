package com.example.springdemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class HeartBeatRequest {
    // json反序列化，jackson识别毫秒时间戳
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    private Date lastActiveTime;

    public Date getLastActiveTime() {
        return lastActiveTime;
    }
}
