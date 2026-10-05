package com.example.springdemo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("`userPassword`")
public class UserPassword {
    private Long id;
    private String password;
    private String position;

    public UserPassword(Long id, String password, String position) {
        this.id = id;
        this.password = password;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public String getPosition() {
        return position;
    }
}
