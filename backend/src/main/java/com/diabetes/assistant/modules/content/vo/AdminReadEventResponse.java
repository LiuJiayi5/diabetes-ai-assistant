package com.diabetes.assistant.modules.content.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminReadEventResponse {

    @JsonProperty("event_id")
    private Integer eventId;

    @JsonProperty("recommendation_id")
    private Integer recommendationId;

    @JsonProperty("source_scenario")
    private String sourceScenario;

    @JsonProperty("source_scenario_label")
    private String sourceScenarioLabel;

    @JsonProperty("read_seconds")
    private Integer readSeconds;

    @JsonProperty("progress_percent")
    private Integer progressPercent;

    @JsonProperty("create_time")
    private String createTime;

    private AdminRecommendationLogResponse.UserBrief user;

    private AdminRecommendationLogResponse.ArticleBrief article;
}
