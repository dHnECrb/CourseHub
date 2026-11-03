package com.example.springdemo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**用于在前端展示在线用户数量、在线会话数量与各角色累计登录次数*/
@Data
@NoArgsConstructor
public class OnlineCountsView {
    private int onlineUserCount;    // 在线用户数量（同一用户多个会话只算 1 个）
    private int onlineSessionCount; // 在线会话数量
    private long adminLoginCount;   // 管理员累计登录次数
    private long studentLoginCount; // 学生累计登录次数
    private long teacherLoginCount; // 教师累计登录次数

    public OnlineCountsView(int onlineUserCount, int onlineSessionCount,
                            long adminLoginCount, long studentLoginCount, long teacherLoginCount) {
        this.onlineUserCount = onlineUserCount;
        this.onlineSessionCount = onlineSessionCount;
        this.adminLoginCount = adminLoginCount;
        this.studentLoginCount = studentLoginCount;
        this.teacherLoginCount = teacherLoginCount;
    }
}
