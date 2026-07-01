package com.diabetes.assistant.modules.content.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.content.service.AdminRecommendationAnalyticsService;
import com.diabetes.assistant.modules.content.vo.AdminRecommendationDashboardResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/content-recommendations")
@RequiredArgsConstructor
public class AdminRecommendationAnalyticsController {

    private final AdminRecommendationAnalyticsService analyticsService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping("/dashboard")
    public ApiResponse<AdminRecommendationDashboardResponse> dashboard(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String scenario,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "knowledge_enhanced", required = false) Boolean knowledgeEnhanced) {
        Integer adminUserId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(analyticsService.getDashboard(adminUserId, page, pageSize, scenario, keyword, knowledgeEnhanced));
    }
}
