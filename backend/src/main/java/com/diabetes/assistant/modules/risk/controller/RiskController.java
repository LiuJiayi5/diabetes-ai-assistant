package com.diabetes.assistant.modules.risk.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.risk.dto.AdminRiskListItem;
import com.diabetes.assistant.modules.risk.dto.RiskDetailResponse;
import com.diabetes.assistant.modules.risk.dto.RiskEntryResponse;
import com.diabetes.assistant.modules.risk.dto.RiskHistoryItem;
import com.diabetes.assistant.modules.risk.dto.RiskPredictResponse;
import com.diabetes.assistant.modules.risk.service.RiskService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping("/entry")
    public ApiResponse<RiskEntryResponse> entry(HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(riskService.getEntry(userId));
    }

    @PostMapping("/predict")
    public ApiResponse<RiskPredictResponse> predictRisk(HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(riskService.predictRisk(userId));
    }

    @GetMapping("/latest")
    public ApiResponse<RiskDetailResponse> getLatestAssessment(HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(riskService.getLatestAssessment(userId));
    }

    @GetMapping("/history")
    public ApiResponse<PageResult<RiskHistoryItem>> getHistory(
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(name = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(riskService.getHistory(userId, page, pageSize, startDate, endDate));
    }

    @GetMapping("/{assessment_id}")
    public ApiResponse<RiskDetailResponse> getAssessmentDetail(
            @PathVariable("assessment_id") Integer assessmentId,
            HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(riskService.getAssessmentDetail(userId, assessmentId));
    }

    @GetMapping("/admin")
    public ApiResponse<PageResult<AdminRiskListItem>> adminListAssessments(
            @RequestParam(name = "user_id", required = false) Integer userId,
            @RequestParam(name = "risk_level", required = false) String riskLevel,
            @RequestParam(name = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            HttpServletRequest request) {
        currentUserUtil.requireAdmin(request);
        return ApiResponse.success(riskService.adminListAssessments(
                userId, riskLevel, startDate, endDate, page, pageSize));
    }

    @GetMapping("/admin/{assessment_id}")
    public ApiResponse<RiskDetailResponse> adminGetAssessmentDetail(
            @PathVariable("assessment_id") Integer assessmentId,
            HttpServletRequest request) {
        currentUserUtil.requireAdmin(request);
        return ApiResponse.success(riskService.adminGetAssessmentDetail(assessmentId));
    }
}
