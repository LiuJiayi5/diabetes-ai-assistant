package com.diabetes.assistant.modules.checkin.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AdminInactiveUserResponse {

    private PatientSummaryResponse patient;
    @JsonProperty("last_checkin_date")
    private LocalDate lastCheckinDate;
    @JsonProperty("recent_total_count")
    private Integer recentTotalCount;
    @JsonProperty("recent_completed_count")
    private Integer recentCompletedCount;
    @JsonProperty("recent_completion_rate")
    private BigDecimal recentCompletionRate;
    @JsonProperty("inactive_days")
    private Integer inactiveDays;
    private String reason;
}
