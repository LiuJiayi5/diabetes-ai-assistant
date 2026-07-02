package com.diabetes.assistant.modules.risk.util;

import com.diabetes.assistant.modules.healthmetric.contract.dto.HealthMetricDTO;
import com.diabetes.assistant.modules.profile.contract.dto.PatientProfileDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskSimilarityUtilTest {

    private PatientProfileDTO sourceProfile;
    private HealthMetricDTO sourceMetric;
    private PatientProfileDTO candidateProfile;
    private HealthMetricDTO candidateMetric;

    @BeforeEach
    void setUp() {
        sourceProfile = profile(46, "male", 172, 96);
        sourceMetric = metric(82.5, 96, new BigDecimal("6.2"));

        candidateProfile = profile(48, "male", 170, 95);
        candidateMetric = metric(81.0, 95, new BigDecimal("6.5"));
    }

    @Test
    void calculateSimilarity_returnsZeroWhenInputMissing() {
        assertEquals(0, RiskSimilarityUtil.calculateSimilarity(null, sourceMetric, candidateProfile, candidateMetric));
    }

    @Test
    void calculateSimilarity_returnsHigherScoreForSimilarProfiles() {
        int similar = RiskSimilarityUtil.calculateSimilarity(
                sourceProfile, sourceMetric, candidateProfile, candidateMetric);

        PatientProfileDTO differentProfile = profile(25, "female", 160, 70);
        HealthMetricDTO differentMetric = metric(55.0, 70, new BigDecimal("5.0"));
        int different = RiskSimilarityUtil.calculateSimilarity(
                sourceProfile, sourceMetric, differentProfile, differentMetric);

        assertTrue(similar > different);
        assertTrue(similar >= 35);
    }

    @Test
    void buildMatchReason_listsMatchedDimensions() {
        String reason = RiskSimilarityUtil.buildMatchReason(
                sourceProfile, sourceMetric, candidateProfile, candidateMetric);
        assertTrue(reason.contains("性别相同") || reason.contains("综合指标相似"));
    }

    @Test
    void round_formatsDecimal() {
        assertEquals(new BigDecimal("6.2"), RiskSimilarityUtil.round(new BigDecimal("6.18")));
    }

    private static PatientProfileDTO profile(int age, String gender, double height, double waist) {
        PatientProfileDTO profile = new PatientProfileDTO();
        profile.setAge(age);
        profile.setGender(gender);
        profile.setHeightCm(BigDecimal.valueOf(height));
        profile.setBaseWaistCm(BigDecimal.valueOf(waist));
        return profile;
    }

    private static HealthMetricDTO metric(double weight, double waist, BigDecimal glucose) {
        HealthMetricDTO metric = new HealthMetricDTO();
        metric.setWeightKg(BigDecimal.valueOf(weight));
        metric.setWaistCm(BigDecimal.valueOf(waist));
        metric.setFastingGlucose(glucose);
        return metric;
    }
}
