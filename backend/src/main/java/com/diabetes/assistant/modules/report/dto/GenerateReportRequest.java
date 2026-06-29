package com.diabetes.assistant.modules.report.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class GenerateReportRequest {

    private String reportType = "personal";

    @Min(7)
    @Max(180)
    private Integer days = 30;
}
