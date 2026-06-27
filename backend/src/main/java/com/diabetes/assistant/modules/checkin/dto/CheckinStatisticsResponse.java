package com.diabetes.assistant.modules.checkin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CheckinStatisticsResponse {

    @JsonProperty("total_days")
    private Integer totalDays;

    @JsonProperty("diet_completion_count")
    private Integer dietCompletionCount;

    @JsonProperty("exercise_completion_count")
    private Integer exerciseCompletionCount;

    @JsonProperty("total_task_count")
    private Integer totalTaskCount;

    @JsonProperty("completed_task_count")
    private Integer completedTaskCount;

    @JsonProperty("completion_rate")
    private BigDecimal completionRate;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;
}
