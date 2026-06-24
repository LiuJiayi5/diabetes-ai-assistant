package com.diabetes.assistant.modules.checkin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CheckinTaskResponse {

    @JsonProperty("checkin_id")
    private Integer checkinId;

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("plan_id")
    private Integer planId;

    @JsonProperty("task_type")
    private String taskType;

    @JsonProperty("task_name")
    private String taskName;

    private String status;
    private String note;

    @JsonProperty("checkin_date")
    private LocalDate checkinDate;

    @JsonProperty("completed_time")
    private LocalDateTime completedTime;
}
