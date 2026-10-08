package com.example.springdemo.dto;

import lombok.Data;

import java.util.List;

@Data
public class SelectedAndCanceledCourses {
    private List<Long> select;
    private List<Long> cancel;
}
