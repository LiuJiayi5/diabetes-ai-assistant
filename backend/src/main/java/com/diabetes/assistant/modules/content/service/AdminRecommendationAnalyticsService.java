package com.diabetes.assistant.modules.content.service;

import com.diabetes.assistant.modules.content.vo.AdminRecommendationDashboardResponse;

public interface AdminRecommendationAnalyticsService {

    AdminRecommendationDashboardResponse getDashboard(Integer adminUserId,
                                                      Integer page,
                                                      Integer pageSize,
                                                      String scenario,
                                                      String keyword,
                                                      Boolean knowledgeEnhanced);
}
