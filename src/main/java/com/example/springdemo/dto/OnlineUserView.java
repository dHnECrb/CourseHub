package com.example.springdemo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**用于在前端表格展示每个在线用户的会话情况*/
@Data
@NoArgsConstructor
public class OnlineUserView {
    private String account;       // 账号
    private String name;          // 姓名（数据库中无对应用户时为 null）
    private long   sessionCount;  // 会话数量
    private long   lastActiveTime; // 最近活跃时间（毫秒时间戳，该用户所有会话的最晚活跃时间）

    public OnlineUserView(String account, String name, long sessionCount, long lastActiveTime) {
        this.account = account;
        this.name = name;
        this.sessionCount = sessionCount;
        this.lastActiveTime = lastActiveTime;
    }
}
