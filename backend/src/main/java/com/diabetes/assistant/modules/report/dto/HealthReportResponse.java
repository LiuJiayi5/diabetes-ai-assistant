package com.diabetes.assistant.modules.report.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HealthReportResponse {

    private Integer reportId;
    private String reportType;
    private String reportTypeLabel;
    private String reportTitle;
    private String reportSummary;
    private String reportMarkdown;
    private Integer completenessScore;
    private List<String> missingItems;
    private String reportStatus;
    private String traceUrl;
    private String qrCodeDataUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
