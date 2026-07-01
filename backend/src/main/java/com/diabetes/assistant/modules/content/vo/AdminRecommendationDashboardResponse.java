package com.diabetes.assistant.modules.content.vo;

import com.diabetes.assistant.common.response.PageResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminRecommendationDashboardResponse {

    private AdminRecommendationOverviewResponse overview;

    private PageResult<AdminRecommendationLogResponse> recommendations;

    @JsonProperty("read_events")
    private PageResult<AdminReadEventResponse> readEvents;
}
