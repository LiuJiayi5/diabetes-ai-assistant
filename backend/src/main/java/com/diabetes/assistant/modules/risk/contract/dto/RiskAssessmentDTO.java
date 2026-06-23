package com.diabetes.assistant.modules.risk.contract.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RiskAssessmentDTO {

    private Integer assessmentId;
    private Integer userId;
    private Integer metricId;
    private String riskLevel;
    private Integer riskScore;
    private String diabetesTypeTendency;
    private String mainRiskFactors;
    private String indicatorAnalysis;
    private String healthAdvice;
    private String medicalWarning;
    private String summary;
    private String callStatus;
    private String errorMessage;
    private LocalDateTime createTime;
}
