package com.example.springdemo.dto;

/**修改密码请求（旧密码、新密码）*/
public record ChangePasswordRequest(String oldPassword, String newPassword) {
}
