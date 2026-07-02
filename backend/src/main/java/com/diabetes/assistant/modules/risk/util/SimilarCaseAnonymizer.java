package com.diabetes.assistant.modules.risk.util;

import org.springframework.util.StringUtils;

public final class SimilarCaseAnonymizer {

    private SimilarCaseAnonymizer() {
    }

    public static String anonymizeInterventionSummary(String summary) {
        if (!StringUtils.hasText(summary)) {
            return summary;
        }
        String result = summary.trim();
        result = result.replaceAll("《[\\u4e00-\\u9fa5]{2,4}的", "《");
        result = result.replaceAll("[\\u4e00-\\u9fa5]{2,4}的(?=[\\u4e00-\\u9fa5])", "");
        result = result.replaceAll("[\\u4e00-\\u9fa5]{2,4}(?=已经|近期|近\\d+天)", "该用户");
        return result;
    }
}
