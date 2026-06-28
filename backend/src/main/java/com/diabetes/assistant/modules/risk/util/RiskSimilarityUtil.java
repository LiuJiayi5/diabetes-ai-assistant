package com.diabetes.assistant.modules.risk.util;

import com.diabetes.assistant.modules.healthmetric.contract.dto.HealthMetricDTO;
import com.diabetes.assistant.modules.healthmetric.util.MetricAbnormalUtils;
import com.diabetes.assistant.modules.profile.contract.dto.PatientProfileDTO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class RiskSimilarityUtil {

    private RiskSimilarityUtil() {
    }

    public static int calculateSimilarity(PatientProfileDTO sourceProfile, HealthMetricDTO sourceMetric,
                                          PatientProfileDTO candidateProfile, HealthMetricDTO candidateMetric) {
        if (sourceProfile == null || candidateProfile == null || sourceMetric == null || candidateMetric == null) {
            return 0;
        }

        double ageScore = numericSimilarity(sourceProfile.getAge(), candidateProfile.getAge(), 20, 4);
        double genderScore = StringUtils.hasText(sourceProfile.getGender())
                && sourceProfile.getGender().equalsIgnoreCase(candidateProfile.getGender()) ? 100 : 40;

        BigDecimal sourceBmi = MetricAbnormalUtils.calculateBmi(sourceProfile.getHeightCm(), sourceMetric.getWeightKg());
        BigDecimal candidateBmi = MetricAbnormalUtils.calculateBmi(candidateProfile.getHeightCm(), candidateMetric.getWeightKg());
        double bmiScore = decimalSimilarity(sourceBmi, candidateBmi, 8, 12);

        double glucoseScore = decimalSimilarity(
                sourceMetric.getFastingGlucose(), candidateMetric.getFastingGlucose(), 3, 20);
        double waistScore = decimalSimilarity(
                resolveWaist(sourceProfile, sourceMetric),
                resolveWaist(candidateProfile, candidateMetric),
                15, 5);

        double weighted = ageScore * 0.15
                + genderScore * 0.10
                + bmiScore * 0.25
                + glucoseScore * 0.30
                + waistScore * 0.20;
        return (int) Math.round(Math.max(0, Math.min(100, weighted)));
    }

    public static String buildMatchReason(PatientProfileDTO sourceProfile, HealthMetricDTO sourceMetric,
                                          PatientProfileDTO candidateProfile, HealthMetricDTO candidateMetric) {
        List<String> reasons = new ArrayList<>();

        if (sourceProfile.getAge() != null && candidateProfile.getAge() != null) {
            int diff = Math.abs(sourceProfile.getAge() - candidateProfile.getAge());
            if (diff <= 5) {
                reasons.add("年龄接近");
            }
        }
        if (StringUtils.hasText(sourceProfile.getGender())
                && sourceProfile.getGender().equalsIgnoreCase(candidateProfile.getGender())) {
            reasons.add("性别相同");
        }

        BigDecimal sourceBmi = MetricAbnormalUtils.calculateBmi(sourceProfile.getHeightCm(), sourceMetric.getWeightKg());
        BigDecimal candidateBmi = MetricAbnormalUtils.calculateBmi(candidateProfile.getHeightCm(), candidateMetric.getWeightKg());
        if (sourceBmi != null && candidateBmi != null
                && sourceBmi.subtract(candidateBmi).abs().compareTo(new BigDecimal("2")) <= 0) {
            reasons.add("BMI 相近");
        }
        if (sourceMetric.getFastingGlucose() != null && candidateMetric.getFastingGlucose() != null
                && sourceMetric.getFastingGlucose().subtract(candidateMetric.getFastingGlucose()).abs()
                .compareTo(new BigDecimal("0.8")) <= 0) {
            reasons.add("空腹血糖接近");
        }
        if (resolveWaist(sourceProfile, sourceMetric) != null && resolveWaist(candidateProfile, candidateMetric) != null
                && resolveWaist(sourceProfile, sourceMetric).subtract(resolveWaist(candidateProfile, candidateMetric)).abs()
                .compareTo(new BigDecimal("5")) <= 0) {
            reasons.add("腰围接近");
        }

        return reasons.isEmpty() ? "综合指标相似" : String.join("、", reasons);
    }

    private static BigDecimal resolveWaist(PatientProfileDTO profile, HealthMetricDTO metric) {
        if (metric.getWaistCm() != null) {
            return metric.getWaistCm();
        }
        return profile.getBaseWaistCm();
    }

    private static double numericSimilarity(Integer source, Integer candidate, double maxDiff, double penalty) {
        if (source == null || candidate == null) {
            return 50;
        }
        double diff = Math.abs(source - candidate);
        return Math.max(0, 100 - Math.min(maxDiff, diff) * penalty);
    }

    private static double decimalSimilarity(BigDecimal source, BigDecimal candidate, double maxDiff, double penalty) {
        if (source == null || candidate == null) {
            return 50;
        }
        double diff = source.subtract(candidate).abs().doubleValue();
        return Math.max(0, 100 - Math.min(maxDiff, diff) * penalty);
    }

    public static BigDecimal round(BigDecimal value) {
        return value == null ? null : value.setScale(1, RoundingMode.HALF_UP);
    }
}
