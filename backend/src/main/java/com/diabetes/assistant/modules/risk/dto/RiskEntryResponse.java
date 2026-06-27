package com.diabetes.assistant.modules.risk.dto;

import lombok.Data;

@Data
public class RiskEntryResponse {

    private RiskDetailResponse latestAssessment;
    private Boolean canPredict;
    private String missingReason;
}
