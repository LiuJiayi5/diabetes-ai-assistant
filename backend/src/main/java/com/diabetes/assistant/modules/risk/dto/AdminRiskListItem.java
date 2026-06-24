package com.diabetes.assistant.modules.risk.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminRiskListItem {

    private Integer assessmentId;
    private Integer userId;
    private String username;
    private String riskLevel;
    private Integer riskScore;
    private String summary;
    private String callStatus;
    private LocalDateTime createTime;
}
