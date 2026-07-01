package com.diabetes.assistant.modules.content.service;

import com.diabetes.assistant.modules.content.dto.ArticleReadEventRequest;
import com.diabetes.assistant.modules.content.vo.ArticleReadEventResponse;
import com.diabetes.assistant.modules.content.vo.ArticleRecommendationResponse;
import com.diabetes.assistant.modules.content.vo.PatientEducationProfileResponse;

import java.util.List;

public interface ArticleRecommendationService {

    List<ArticleRecommendationResponse> recommend(Integer userId, String scenario, Integer limit);

    PatientEducationProfileResponse getPatientEducationProfile(Integer userId);

    ArticleReadEventResponse recordReadEvent(Integer userId, ArticleReadEventRequest request);
}
