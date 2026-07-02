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

    @DecimalMin(value = "20.0", message = "体重应在 20～300 kg 之间")
    @DecimalMax(value = "300.0", message = "体重应在 20～300 kg 之间")
    private BigDecimal weightKg;

    @DecimalMin(value = "30.0", message = "腰围应在 30～200 cm 之间")
    @DecimalMax(value = "200.0", message = "腰围应在 30～200 cm 之间")
    private BigDecimal waistCm;

    @Min(value = 60, message = "收缩压应在 60～260 mmHg 之间")
    @Max(value = 260, message = "收缩压应在 60～260 mmHg 之间")
    private Integer systolicBp;

    @Min(value = 30, message = "舒张压应在 30～180 mmHg 之间")
    @Max(value = 180, message = "舒张压应在 30～180 mmHg 之间")
    private Integer diastolicBp;

    @DecimalMin(value = "1.0", message = "空腹血糖应在 1.0～30.0 mmol/L 之间")
    @DecimalMax(value = "30.0", message = "空腹血糖应在 1.0～30.0 mmol/L 之间")
    private BigDecimal fastingGlucose;

    @DecimalMin(value = "1.0", message = "餐后血糖应在 1.0～35.0 mmol/L 之间")
    @DecimalMax(value = "35.0", message = "餐后血糖应在 1.0～35.0 mmol/L 之间")
    private BigDecimal postprandialGlucose;

    @DecimalMin(value = "3.0", message = "糖化血红蛋白应在 3.0～20.0 之间")
    @DecimalMax(value = "20.0", message = "糖化血红蛋白应在 3.0～20.0 之间")
    private BigDecimal hba1c;

    private String dietStatus;
    private String exerciseStatus;
    private Boolean skipInterventionReview;

    @NotNull(message = "请填写记录日期，格式 YYYY-MM-DD")
    private LocalDate recordedAt;
}
