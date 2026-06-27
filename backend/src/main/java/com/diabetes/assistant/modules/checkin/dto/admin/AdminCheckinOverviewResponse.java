package com.diabetes.assistant.modules.checkin.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class AdminCheckinOverviewResponse {

    @JsonProperty("start_date")
    private LocalDate startDate;
    @JsonProperty("end_date")
    private LocalDate endDate;
    @JsonProperty("total_task_count")
    private Integer totalTaskCount;
    @JsonProperty("completed_task_count")
    private Integer completedTaskCount;
    @JsonProperty("unfinished_task_count")
    private Integer unfinishedTaskCount;
    @JsonProperty("diet_total_count")
    private Integer dietTotalCount;
    @JsonProperty("diet_completed_count")
    private Integer dietCompletedCount;
    @JsonProperty("exercise_total_count")
    private Integer exerciseTotalCount;
    @JsonProperty("exercise_completed_count")
    private Integer exerciseCompletedCount;
    @JsonProperty("completion_rate")
    private BigDecimal completionRate;
    @JsonProperty("daily_stats")
    private List<DailyStat> dailyStats;

    @Data
    public static class DailyStat {
        private LocalDate date;
        @JsonProperty("total_count")
        private Integer totalCount;
        @JsonProperty("completed_count")
        private Integer completedCount;
        @JsonProperty("completion_rate")
        private BigDecimal completionRate;
    }
}
