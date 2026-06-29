package com.diabetes.assistant.modules.checkin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckinTodayResponse {

    private List<CheckinTaskResponse> list;
    private String message;

    @JsonProperty("plan_id")
    private Integer planId;

    @JsonProperty("plan_title")
    private String planTitle;

    @JsonProperty("plan_goal")
    private String planGoal;

    @JsonProperty("plan_summary")
    private String planSummary;

    @JsonProperty("plan_create_time")
    private LocalDateTime planCreateTime;

    @JsonProperty("plan_day")
    private Integer planDay;

    @JsonProperty("total_plan_days")
    private Integer totalPlanDays;

    @JsonProperty("is_plan_expired")
    private Boolean planExpired;

    @JsonProperty("today_schedule")
    private Map<String, Object> todaySchedule;

    @JsonProperty("today_focus")
    private String todayFocus;
}
