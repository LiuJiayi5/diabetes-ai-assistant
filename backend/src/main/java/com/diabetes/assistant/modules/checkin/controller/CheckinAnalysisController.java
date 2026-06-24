package com.diabetes.assistant.modules.checkin.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.checkin.dto.CheckinAnalysisGenerateRequest;
import com.diabetes.assistant.modules.checkin.dto.CheckinAnalysisResponse;
import com.diabetes.assistant.modules.checkin.service.CheckinAnalysisService;
import jakarta.servlet.http.HttpServletRequest;
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
@RequiredArgsConstructor
public class CheckinAnalysisController {

    private final CheckinAnalysisService checkinAnalysisService;
    private final CurrentUserUtil currentUserUtil;

    @PostMapping("/api/ai/checkin-analysis")
    public ApiResponse<CheckinAnalysisResponse> generate(
            HttpServletRequest request,
            @RequestBody(required = false) CheckinAnalysisGenerateRequest generateRequest) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        Integer period = generateRequest == null ? null : generateRequest.getPeriod();
        return ApiResponse.success(checkinAnalysisService.generateAnalysis(userId, period));
    }

    @GetMapping("/api/checkin-analysis/latest")
    public ApiResponse<CheckinAnalysisResponse> latest(HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(checkinAnalysisService.getLatestAnalysis(userId));
    }

    @GetMapping("/api/checkin-analysis/history")
    public ApiResponse<PageResult<CheckinAnalysisResponse>> history(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(checkinAnalysisService.listHistory(userId, page, pageSize, startDate, endDate));
    }
}
