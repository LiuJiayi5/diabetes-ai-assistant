package com.diabetes.assistant.modules.checkin.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.checkin.dto.CheckinHistoryQuery;
import com.diabetes.assistant.modules.checkin.dto.CheckinStatisticsResponse;
import com.diabetes.assistant.modules.checkin.dto.CheckinSubmitRequest;
import com.diabetes.assistant.modules.checkin.dto.CheckinTaskResponse;
import com.diabetes.assistant.modules.checkin.dto.CheckinTodayResponse;
import com.diabetes.assistant.modules.checkin.service.CheckinService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/checkins")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping("/entry")
    public ApiResponse<String> entry() {
        return ApiResponse.success(checkinService.entry());
    }

    @GetMapping("/today")
    public ApiResponse<CheckinTodayResponse> today(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(checkinService.getTodayTasks(userId, date));
    }

    @PostMapping
    public ApiResponse<CheckinTaskResponse> submit(
            HttpServletRequest request,
            @Valid @RequestBody CheckinSubmitRequest submitRequest) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(checkinService.submitCheckin(userId, submitRequest));
    }

    @GetMapping("/history")
    public ApiResponse<PageResult<CheckinTaskResponse>> history(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "task_type", required = false) String taskType,
            @RequestParam(required = false) String status) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        CheckinHistoryQuery query = new CheckinHistoryQuery();
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setTaskType(taskType);
        query.setStatus(status);
        return ApiResponse.success(checkinService.listHistory(userId, query));
    }

    @GetMapping("/statistics")
    public ApiResponse<CheckinStatisticsResponse> statistics(
            HttpServletRequest request,
            @RequestParam(defaultValue = "7") Integer period) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(checkinService.getStatistics(userId, period));
    }
}
