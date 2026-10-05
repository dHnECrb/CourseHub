package com.example.springdemo.entity;

import lombok.Getter;

@Getter
public enum UserPosition {
    ADMIN("admin",1),
    STUDENT("student",2),
    TEACHER("teacher",3);

    private final String string;
    private final int code;

    UserPosition(String string, int code) {
        this.string=string;
        this.code=code;
    }

    /**根据代码取出String类型的职位
     * @param code 代码
     * @return 职位
     * @throws IllegalArgumentException 当代码非法时
     * */
    public static String getStringFromInt(int code){
        for(UserPosition userPosition : UserPosition.values()){
            if(userPosition.code==code){
                return userPosition.string;
            }
        }
        throw new IllegalArgumentException("错误的代码。:"+code);
    }
}
