package com.diabetes.assistant.modules.interventionreview.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.interventionreview.dto.InterventionReviewResponse;
import com.diabetes.assistant.modules.interventionreview.service.InterventionReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InterventionReviewController {

    private final InterventionReviewService interventionReviewService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping("/api/intervention-reviews/latest")
    public ApiResponse<InterventionReviewResponse> latest(HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(interventionReviewService.getLatest(userId));
    }

    @GetMapping("/api/intervention-reviews/history")
    public ApiResponse<PageResult<InterventionReviewResponse>> history(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(interventionReviewService.listHistory(userId, page, pageSize));
    }

    @GetMapping("/api/admin/intervention-reviews")
    public ApiResponse<PageResult<InterventionReviewResponse>> adminList(
            HttpServletRequest request,
            @RequestParam(name = "user_id", required = false) Integer userId,
            @RequestParam(name = "intervention_level", required = false) String interventionLevel,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize) {
        Integer adminUserId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(interventionReviewService.listAdmin(adminUserId, userId, interventionLevel, page, pageSize));
    }
}
