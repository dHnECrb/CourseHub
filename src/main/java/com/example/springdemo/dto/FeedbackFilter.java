package com.example.springdemo.dto;

import java.time.LocalDate;

public record FeedbackFilter(LocalDate startDate, LocalDate endDate, Boolean isRead, Boolean isDeleted) {
    public static FeedbackFilter defaultFilter() {
        return new FeedbackFilter(null, null, false, false);
    }
}
