package com.diabetes.assistant.modules.lifeplan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GenerateLifePlanRequest {

    @JsonProperty("plan_goal")
    private String planGoal;

    @JsonProperty("avoid_items")
    private List<String> avoidItems;

    @JsonProperty("plan_days")
    private Integer planDays;

    // Intentionally ignored if the frontend sends it.
    @JsonProperty("user_id")
    private Integer userId;
}
