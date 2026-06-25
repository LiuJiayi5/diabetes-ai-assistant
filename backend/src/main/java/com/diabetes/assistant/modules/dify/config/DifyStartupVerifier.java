package com.diabetes.assistant.modules.dify.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class DifyStartupVerifier implements ApplicationRunner {

    private final DifyProperties difyProperties;

    @Override
    public void run(ApplicationArguments args) {
        log.info(
                "Dify configuration: baseUrl={}, lifePlanKey={}, riskPredictKey={}, aiDoctorKey={}, checkinAnalysisKey={}",
                valueOrMissing(difyProperties.getBaseUrl()),
                keyStatus(difyProperties.getLifePlanApiKey()),
                keyStatus(difyProperties.getRiskPredictApiKey()),
                keyStatus(difyProperties.getAiDoctorApiKey()),
                keyStatus(difyProperties.getCheckinAnalysisApiKey())
        );
        if (!hasUsableKey(difyProperties.getLifePlanApiKey())) {
            log.warn("Dify life-plan workflow key is not configured. Real AI life-plan generation will not be available.");
        }
    }

    private String valueOrMissing(String value) {
        return StringUtils.hasText(value) ? value : "missing";
    }

    private String keyStatus(String key) {
        return hasUsableKey(key) ? "configured" : "missing";
    }

    private boolean hasUsableKey(String key) {
        return StringUtils.hasText(key) && !key.startsWith("your_");
    }
}
