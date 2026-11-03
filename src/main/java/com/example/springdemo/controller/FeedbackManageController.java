package com.example.springdemo.controller;

import com.example.springdemo.dto.FeedbackFilter;
import com.example.springdemo.dto.RequestResult;
import com.example.springdemo.service.FeedbackService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/admin/feedback_manage")
public class FeedbackManageController {

    @Resource
    private FeedbackService feedbackService;

    @GetMapping
    public ModelAndView index(@NonNull HttpSession httpSession,
                              @NonNull HttpServletResponse response,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                              @RequestParam(defaultValue = "unread") String readStatus,
                              @RequestParam(defaultValue = "not_deleted") String deleteStatus) throws IOException {
        RequestResult requestResult = feedbackService.indexAdmin(httpSession);
        if (!requestResult.isSucceed()) {
            response.sendError(requestResult.code(), requestResult.string());
            return null;
        }
        FeedbackFilter filter = createFilter(startDate, endDate, readStatus, deleteStatus);
        ModelAndView modelAndView = new ModelAndView("feedback_manage");
        modelAndView.addObject("feedbackList", feedbackService.getFeedback(filter));
        modelAndView.addObject("startDate", startDate);
        modelAndView.addObject("endDate", endDate);
        modelAndView.addObject("readStatus", readStatus);
        modelAndView.addObject("deleteStatus", deleteStatus);
        return modelAndView;
    }

    @PostMapping("/read")
    public RequestResult markRead(@NonNull HttpSession httpSession,
                                  @RequestParam(required = false) Long feedbackId,
                                  @RequestParam boolean read) {
        return feedbackService.markRead(httpSession, feedbackId, read);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@NonNull HttpSession httpSession,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                         @RequestParam(defaultValue = "unread") String readStatus,
                                         @RequestParam(defaultValue = "not_deleted") String deleteStatus) {
        RequestResult permission = feedbackService.indexAdmin(httpSession);
        if (!permission.isSucceed()) {
            return ResponseEntity.status(permission.code()).build();
        }
        FeedbackFilter filter = createFilter(startDate, endDate, readStatus, deleteStatus);
        String filename = "feedback_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename(filename).build());
        return new ResponseEntity<>(feedbackService.exportFeedback(filter), headers, HttpStatus.OK);
    }

    private static FeedbackFilter createFilter(LocalDate startDate, LocalDate endDate,
                                               String readStatus, String deleteStatus) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
        return new FeedbackFilter(startDate, endDate,
                parseStatus(readStatus, "read", "unread"),
                parseStatus(deleteStatus, "deleted", "not_deleted"));
    }

    private static Boolean parseStatus(String status, String trueValue, String falseValue) {
        if ("all".equals(status)) {
            return null;
        }
        if (trueValue.equals(status)) {
            return true;
        }
        if (falseValue.equals(status)) {
            return false;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的筛选条件");
    }
}
