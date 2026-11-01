package com.example.springdemo.dto;

import lombok.Data;

/**管理员创建用户时提交的一行用户信息（来自单个表单或 CSV 导入）
 * id 必填；password 留空则初始密码与 id 相同；其余字段留空存 null*/
@Data
public class CreatedUserView {
    private Long id;
    private String name;
    private Integer age;
    private String email;
    private String position;
    private String password;
}
