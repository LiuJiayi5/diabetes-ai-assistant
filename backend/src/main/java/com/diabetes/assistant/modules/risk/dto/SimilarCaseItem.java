package com.diabetes.assistant.modules.risk.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SimilarCaseItem {

    private Integer userId;
    private String username;
    private Integer age;
    private String gender;
    private Integer similarityScore;
    private String riskLevel;
    private Integer riskScore;
    private String summary;
    private BigDecimal fastingGlucose;
    private BigDecimal weightKg;
    private BigDecimal waistCm;
    private String matchReason;
}
