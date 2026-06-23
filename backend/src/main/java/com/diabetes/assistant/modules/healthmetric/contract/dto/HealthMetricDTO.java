package com.diabetes.assistant.modules.healthmetric.contract.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class HealthMetricDTO {

    private Integer metricId;
    private Integer userId;
    private BigDecimal weightKg;
    private BigDecimal waistCm;
    private Integer systolicBp;
    private Integer diastolicBp;
    private BigDecimal fastingGlucose;
    private BigDecimal postprandialGlucose;
    private BigDecimal hba1c;
    private String dietStatus;
    private String exerciseStatus;
    private String metricSummary;
    private LocalDate recordedAt;
    private LocalDateTime createTime;
}
