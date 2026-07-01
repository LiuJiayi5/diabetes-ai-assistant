package com.diabetes.assistant.modules.content.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.content.dto.ArticleReadEventRequest;
import com.diabetes.assistant.modules.content.service.ArticleRecommendationService;
import com.diabetes.assistant.modules.content.vo.ArticleReadEventResponse;
import com.diabetes.assistant.modules.content.vo.ArticleRecommendationResponse;
import com.diabetes.assistant.modules.content.vo.PatientEducationProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/content-recommendations")
@RequiredArgsConstructor
public class ArticleRecommendationController {

    private final ArticleRecommendationService recommendationService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping
    public ApiResponse<List<ArticleRecommendationResponse>> recommend(
            HttpServletRequest request,
            @RequestParam(defaultValue = "home") String scenario,
            @RequestParam(defaultValue = "6") Integer limit) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(recommendationService.recommend(userId, scenario, limit));
    }

    @GetMapping("/profile")
    public ApiResponse<PatientEducationProfileResponse> profile(HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(recommendationService.getPatientEducationProfile(userId));
    }

    @PostMapping("/read-events")
    public ApiResponse<ArticleReadEventResponse> recordReadEvent(
            HttpServletRequest request,
            @RequestBody ArticleReadEventRequest readRequest) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(recommendationService.recordReadEvent(userId, readRequest));
    }
}
