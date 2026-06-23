package com.diabetes.assistant.modules.profile.contract.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PatientProfileDTO {

    private Integer profileId;
    private Integer userId;
    private Integer age;
    private String gender;
    private BigDecimal heightCm;
    private BigDecimal baseWeightKg;
    private BigDecimal baseWaistCm;
    private String familyHistory;
    private String chronicHistory;
    private String allergyHistory;
    private String profileSummary;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
