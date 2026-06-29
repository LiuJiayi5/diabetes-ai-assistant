package com.diabetes.assistant.modules.interventionreview.service;

import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.interventionreview.dto.InterventionReviewResponse;

public interface InterventionReviewService {

    void tryAutoReview(Integer userId, String triggerType, String triggerReason);

    InterventionReviewResponse getLatest(Integer userId);

    PageResult<InterventionReviewResponse> listHistory(Integer userId, Integer page, Integer pageSize);

    PageResult<InterventionReviewResponse> listAdmin(Integer adminUserId, Integer userId, String interventionLevel,
                                                     Integer page, Integer pageSize);
}
