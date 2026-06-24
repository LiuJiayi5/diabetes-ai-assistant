package com.diabetes.assistant.modules.risk.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RiskHistoryItem {

    private Integer assessmentId;
    private String riskLevel;
    private Integer riskScore;
    private String summary;
    private String callStatus;
    private LocalDateTime createTime;
}
