package com.diabetes.assistant.modules.content.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PatientEducationProfileResponse {

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("profile_tags")
    private List<String> profileTags;

    @JsonProperty("metric_tags")
    private List<String> metricTags;

    @JsonProperty("plan_tags")
    private List<String> planTags;

    @JsonProperty("review_tags")
    private List<String> reviewTags;

    @JsonProperty("read_tags")
    private List<String> readTags;

    @JsonProperty("summary")
    private String summary;
}
