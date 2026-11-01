package com.example.springdemo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**用于在前端展示在线用户数量与在线会话数量*/
@Data
@NoArgsConstructor
public class OnlineCountsView {
    private int onlineUserCount;    // 在线用户数量（同一用户多个会话只算 1 个）
    private int onlineSessionCount; // 在线会话数量

    public OnlineCountsView(int onlineUserCount, int onlineSessionCount) {
        this.onlineUserCount = onlineUserCount;
        this.onlineSessionCount = onlineSessionCount;
    }
}
