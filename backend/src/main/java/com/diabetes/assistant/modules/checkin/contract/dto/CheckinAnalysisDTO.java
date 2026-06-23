package com.diabetes.assistant.modules.checkin.contract.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CheckinAnalysisDTO {

    private Integer analysisId;
    private Integer userId;
    private Integer planId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Integer dietCompletionCount;
    private Integer exerciseCompletionCount;
    private BigDecimal completionRate;
    private Integer habitScore;
    private String dietSummary;
    private String exerciseSummary;
    private String lifeEvaluation;
    private String mainProblems;
    private String improvementSuggestions;
    private String nextFocus;
    private String summary;
    private String inputSummary;
    private String callStatus;
    private String errorMessage;
    private LocalDateTime createTime;
}
