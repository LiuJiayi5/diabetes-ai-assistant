package com.diabetes.assistant.modules.healthmetric.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MetricTrendResponse {

    private List<MetricTrendSeries> series;

    @Data
    public static class MetricTrendSeries {
        private String key;
        private String label;
        private String unit;
        private List<MetricTrendPoint> points;
    }

    @Data
    public static class MetricTrendPoint {
        private LocalDateTime recordedAt;
        private BigDecimal value;
    }
}
