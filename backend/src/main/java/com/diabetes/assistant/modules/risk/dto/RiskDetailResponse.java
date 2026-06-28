package com.diabetes.assistant.modules.risk.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RiskDetailResponse {

    private Integer assessmentId;
    private Integer userId;
    private String riskLevel;
    private Integer riskScore;
    private String diabetesTypeTendency;
    private List<String> mainRiskFactors;
    private String indicatorAnalysis;
    private String healthAdvice;
    private String medicalWarning;
    private String summary;
    private String requestSummary;
    private String callStatus;
    private String errorMessage;
    private List<String> referenceSources;
    private LocalDateTime createTime;
}
