package com.diabetes.assistant.modules.profile.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaveProfileRequest {

    @NotNull
    @Min(1)
    @Max(120)
    private Integer age;

    @NotBlank
    private String gender;

    @NotNull
    @DecimalMin("50.0")
    @DecimalMax("250.0")
    private BigDecimal heightCm;

    @NotNull
    @DecimalMin("20.0")
    @DecimalMax("300.0")
    private BigDecimal baseWeightKg;

    @DecimalMin("30.0")
    @DecimalMax("200.0")
    private BigDecimal baseWaistCm;

    private String familyHistory;
    private String chronicHistory;
    private String allergyHistory;
}
