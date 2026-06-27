package com.diabetes.assistant.modules.healthmetric.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SaveMetricRequest {

    @DecimalMin("20.0")
    @DecimalMax("300.0")
    private BigDecimal weightKg;

    @DecimalMin("30.0")
    @DecimalMax("200.0")
    private BigDecimal waistCm;

    @Min(60)
    @Max(260)
    private Integer systolicBp;

    @Min(30)
    @Max(180)
    private Integer diastolicBp;

    @DecimalMin("1.0")
    @DecimalMax("30.0")
    private BigDecimal fastingGlucose;

    @DecimalMin("1.0")
    @DecimalMax("35.0")
    private BigDecimal postprandialGlucose;

    @DecimalMin("3.0")
    @DecimalMax("20.0")
    private BigDecimal hba1c;

    private String dietStatus;
    private String exerciseStatus;

    @NotNull
    private LocalDate recordedAt;
}
