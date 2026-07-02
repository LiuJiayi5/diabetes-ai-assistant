package com.diabetes.assistant.modules.risk.dto;

import lombok.Data;

@Data
public class PatientSimilarCaseItem {

    private String caseLabel;
    private Integer similarityScore;
    private String matchReason;
    private String interventionSummary;
    private String outcomeSummary;
    private Integer checkinCompletionRate;
}
