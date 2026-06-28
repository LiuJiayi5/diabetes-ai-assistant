package com.diabetes.assistant.modules.healthmetric.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.healthmetric.contract.dto.HealthMetricDTO;
import com.diabetes.assistant.modules.healthmetric.dto.AdminMetricListItem;
import com.diabetes.assistant.modules.healthmetric.dto.MetricTrendResponse;
import com.diabetes.assistant.modules.healthmetric.dto.SaveMetricRequest;
import com.diabetes.assistant.modules.healthmetric.dto.SaveMetricResponse;
import com.diabetes.assistant.modules.healthmetric.service.HealthMetricService;
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
@RequestMapping("/api/health-metric")
@RequiredArgsConstructor
public class HealthMetricController {

    private final HealthMetricService healthMetricService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping("/entry")
    public ApiResponse<HealthMetricDTO> entry(HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(healthMetricService.getEntry(userId));
    }

    @PostMapping
    public ApiResponse<SaveMetricResponse> saveMetric(@Valid @RequestBody SaveMetricRequest body,
                                                      HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(healthMetricService.saveMetric(userId, body));
    }

    @GetMapping("/latest")
    public ApiResponse<HealthMetricDTO> getLatestMetric(HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(healthMetricService.getLatestMetric(userId));
    }

    @GetMapping("/history")
    public ApiResponse<PageResult<HealthMetricDTO>> getHistory(
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(name = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(healthMetricService.getHistory(userId, page, pageSize, startDate, endDate));
    }

    @GetMapping("/trends")
    public ApiResponse<MetricTrendResponse> getMetricTrends(HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(healthMetricService.getMetricTrends(userId));
    }

    @GetMapping("/admin/trends")
    public ApiResponse<MetricTrendResponse> adminGetMetricTrends(
            @RequestParam(name = "user_id") Integer userId,
            HttpServletRequest request) {
        currentUserUtil.requireAdmin(request);
        return ApiResponse.success(healthMetricService.adminGetMetricTrends(userId));
    }

    @GetMapping("/admin")
    public ApiResponse<PageResult<AdminMetricListItem>> adminListMetrics(
            @RequestParam(name = "user_id", required = false) Integer userId,
            @RequestParam(name = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "abnormal_only", required = false) String abnormalOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            HttpServletRequest request) {
        currentUserUtil.requireAdmin(request);
        return ApiResponse.success(healthMetricService.adminListMetrics(
                userId, startDate, endDate, abnormalOnly, page, pageSize));
    }

    @GetMapping("/admin/abnormal")
    public ApiResponse<PageResult<AdminMetricListItem>> adminListAbnormalMetrics(
            @RequestParam(name = "user_id", required = false) Integer userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            HttpServletRequest request) {
        currentUserUtil.requireAdmin(request);
        return ApiResponse.success(healthMetricService.adminListAbnormalMetrics(userId, page, pageSize));
    }
}
