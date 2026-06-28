package com.diabetes.assistant.modules.dify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "dify")
public class DifyProperties {

    private String baseUrl;
    private String riskPredictApiKey;
    private String aiDoctorApiKey;
    private String lifePlanApiKey;
    private String checkinAnalysisApiKey;
    private String comprehensiveReportApiKey;
}
