package com.example.springdemo.dto;


public final class UserToLogin{
    String id;
    String password;

    public UserToLogin(String id, String password) {
        this.id = id;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString(){
        return "id: "+ id +"; password: "+password;
    }
}
