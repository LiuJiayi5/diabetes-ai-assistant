package com.diabetes.assistant.modules.checkin.controller;

import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminAnalysisQuery;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminApiCallLogResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminCheckinAnalysisResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminCheckinOverviewResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminCheckinQuery;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminCheckinRecordResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminInactiveUserResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminLogQuery;
import com.diabetes.assistant.modules.checkin.service.AdminCheckinService;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/checkins")
@RequiredArgsConstructor
public class AdminCheckinController {

    private final AdminCheckinService adminCheckinService;
    private final CurrentUserUtil currentUserUtil;
    private final UserQueryApi userQueryApi;

    @GetMapping("/records")
    public ApiResponse<PageResult<AdminCheckinRecordResponse>> records(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "user_id", required = false) Integer userId,
            @RequestParam(name = "patient_keyword", required = false) String patientKeyword,
            @RequestParam(name = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "task_type", required = false) String taskType,
            @RequestParam(required = false) String status) {
        requireAdmin(request);
        AdminCheckinQuery query = new AdminCheckinQuery();
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setUserId(userId);
        query.setPatientKeyword(patientKeyword);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setTaskType(taskType);
        query.setStatus(status);
        return ApiResponse.success(adminCheckinService.listRecords(query));
    }

    @GetMapping("/records/{checkinId}")
    public ApiResponse<AdminCheckinRecordResponse> recordDetail(
            HttpServletRequest request,
            @PathVariable Integer checkinId) {
        requireAdmin(request);
        return ApiResponse.success(adminCheckinService.getRecordDetail(checkinId));
    }

    @GetMapping("/overview")
    public ApiResponse<AdminCheckinOverviewResponse> overview(
            HttpServletRequest request,
            @RequestParam(name = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "user_id", required = false) Integer userId) {
        requireAdmin(request);
        return ApiResponse.success(adminCheckinService.getOverview(startDate, endDate, userId));
    }

    @GetMapping("/analyses")
    public ApiResponse<PageResult<AdminCheckinAnalysisResponse>> analyses(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "user_id", required = false) Integer userId,
            @RequestParam(name = "patient_keyword", required = false) String patientKeyword,
            @RequestParam(name = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "call_status", required = false) String callStatus) {
        requireAdmin(request);
        AdminAnalysisQuery query = new AdminAnalysisQuery();
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setUserId(userId);
        query.setPatientKeyword(patientKeyword);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setCallStatus(callStatus);
        return ApiResponse.success(adminCheckinService.listAnalyses(query));
    }

    @GetMapping("/analyses/{analysisId}")
    public ApiResponse<AdminCheckinAnalysisResponse> analysisDetail(
            HttpServletRequest request,
            @PathVariable Integer analysisId) {
        requireAdmin(request);
        return ApiResponse.success(adminCheckinService.getAnalysisDetail(analysisId));
    }

    @GetMapping("/inactive-users")
    public ApiResponse<List<AdminInactiveUserResponse>> inactiveUsers(
            HttpServletRequest request,
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(defaultValue = "20") Integer limit) {
        requireAdmin(request);
        return ApiResponse.success(adminCheckinService.listInactiveUsers(days, limit));
    }

    @GetMapping("/analysis-logs")
    public ApiResponse<PageResult<AdminApiCallLogResponse>> analysisLogs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "user_id", required = false) Integer userId,
            @RequestParam(name = "patient_keyword", required = false) String patientKeyword,
            @RequestParam(name = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "call_status", required = false) String callStatus) {
        requireAdmin(request);
        AdminLogQuery query = new AdminLogQuery();
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setUserId(userId);
        query.setPatientKeyword(patientKeyword);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setCallStatus(callStatus);
        return ApiResponse.success(adminCheckinService.listAnalysisLogs(query));
    }

    @GetMapping("/analysis-logs/{logId}")
    public ApiResponse<AdminApiCallLogResponse> analysisLogDetail(
            HttpServletRequest request,
            @PathVariable Integer logId) {
        requireAdmin(request);
        return ApiResponse.success(adminCheckinService.getAnalysisLogDetail(logId));
    }

    private void requireAdmin(HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        if (!userQueryApi.isAdmin(userId)) {
            throw new BusinessException(403, "仅管理员可访问模块8管理端数据");
        }
    }
}
