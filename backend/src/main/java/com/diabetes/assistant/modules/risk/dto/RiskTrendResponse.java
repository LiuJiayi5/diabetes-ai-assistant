package com.diabetes.assistant.modules.risk.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RiskTrendResponse {

    private List<RiskTrendPoint> points;

    @Data
    public static class RiskTrendPoint {
        private Integer assessmentId;
        private LocalDateTime recordedAt;
        private Integer riskScore;
        private String riskLevel;
    }
}
