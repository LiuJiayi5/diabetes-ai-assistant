package com.diabetes.assistant.modules.lifeplan.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LifePlanResponse {

    @JsonProperty("plan_id")
    private Integer planId;

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("profile_id")
    private Integer profileId;

    @JsonProperty("metric_id")
    private Integer metricId;

    @JsonProperty("assessment_id")
    private Integer assessmentId;

    @JsonProperty("plan_title")
    private String planTitle;

    @JsonProperty("plan_goal")
    private String planGoal;

    @JsonProperty("input_summary")
    private String inputSummary;

    @JsonProperty("plan_json")
    private Object planJson;

    @JsonProperty("checkin_tasks_json")
    private Object checkinTasksJson;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("status")
    private String status;

    @JsonProperty("call_status")
    private String callStatus;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("risk_score")
    private Integer riskScore;

    @JsonProperty("create_time")
    private String createTime;

    @JsonProperty("update_time")
    private String updateTime;
}
