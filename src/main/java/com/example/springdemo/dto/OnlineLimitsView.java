package com.example.springdemo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**用于在前端展示与设置在线用户数量上限、单用户会话数量上限（0 表示不限制）*/
@Data
@NoArgsConstructor
public class OnlineLimitsView {
    private int maxOnlineUserCount;    // 在线用户数量上限
    private int maxSessionCountPerUser; // 单个用户的会话数量上限

    public OnlineLimitsView(int maxOnlineUserCount, int maxSessionCountPerUser) {
        this.maxOnlineUserCount = maxOnlineUserCount;
        this.maxSessionCountPerUser = maxSessionCountPerUser;
    }
}
