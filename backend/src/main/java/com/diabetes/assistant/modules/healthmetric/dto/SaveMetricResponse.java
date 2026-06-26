package com.diabetes.assistant.modules.healthmetric.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SaveMetricResponse {

    private Integer metricId;
    private LocalDateTime createTime;
}
