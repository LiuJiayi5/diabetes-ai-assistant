package com.diabetes.assistant.modules.risk.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.risk.dto.AdminRiskListItem;
import com.diabetes.assistant.modules.risk.dto.RiskDetailResponse;
import com.diabetes.assistant.modules.risk.dto.RiskEntryResponse;
import com.diabetes.assistant.modules.risk.dto.RiskHistoryItem;
import com.diabetes.assistant.modules.risk.dto.RiskPredictResponse;
import com.diabetes.assistant.modules.risk.service.RiskService;
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

    @GetMapping("/entry")
    public ApiResponse<RiskEntryResponse> entry() {
        return ApiResponse.success(riskService.getEntry());
    }

    @PostMapping("/predict")
    public ApiResponse<RiskPredictResponse> predictRisk() {
        return ApiResponse.success(riskService.predictRisk());
    }

    @GetMapping("/latest")
    public ApiResponse<RiskDetailResponse> getLatestAssessment() {
        return ApiResponse.success(riskService.getLatestAssessment());
    }

    @GetMapping("/history")
    public ApiResponse<PageResult<RiskHistoryItem>> getHistory(
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(name = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(riskService.getHistory(page, pageSize, startDate, endDate));
    }

    @GetMapping("/{assessment_id}")
    public ApiResponse<RiskDetailResponse> getAssessmentDetail(
            @PathVariable("assessment_id") Integer assessmentId) {
        return ApiResponse.success(riskService.getAssessmentDetail(assessmentId));
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
            @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(riskService.adminListAssessments(
                userId, riskLevel, startDate, endDate, page, pageSize));
    }

    @GetMapping("/admin/{assessment_id}")
    public ApiResponse<RiskDetailResponse> adminGetAssessmentDetail(
            @PathVariable("assessment_id") Integer assessmentId) {
        return ApiResponse.success(riskService.adminGetAssessmentDetail(assessmentId));
    }
}
