package com.diabetes.assistant.modules.checkin.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PlanSummaryResponse {

    @JsonProperty("plan_id")
    private Integer planId;
    @JsonProperty("plan_title")
    private String planTitle;
    @JsonProperty("plan_goal")
    private String planGoal;
    private String summary;
    private String status;
}
