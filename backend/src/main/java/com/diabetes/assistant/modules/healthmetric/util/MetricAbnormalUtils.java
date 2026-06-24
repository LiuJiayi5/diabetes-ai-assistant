package com.diabetes.assistant.modules.healthmetric.util;

import com.diabetes.assistant.modules.healthmetric.entity.HealthMetric;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MetricAbnormalUtils {

    private MetricAbnormalUtils() {
    }

    public static boolean isAbnormal(HealthMetric metric) {
        return isFastingGlucoseAbnormal(metric.getFastingGlucose())
                || isPostprandialGlucoseAbnormal(metric.getPostprandialGlucose())
                || isSystolicBpAbnormal(metric.getSystolicBp())
                || isDiastolicBpAbnormal(metric.getDiastolicBp());
    }

    public static boolean isFastingGlucoseAbnormal(BigDecimal value) {
        return value != null && (value.compareTo(new BigDecimal("3.9")) < 0
                || value.compareTo(new BigDecimal("7.0")) > 0);
    }

    public static boolean isPostprandialGlucoseAbnormal(BigDecimal value) {
        return value != null && value.compareTo(new BigDecimal("11.1")) > 0;
    }

    public static boolean isSystolicBpAbnormal(Integer value) {
        return value != null && (value < 90 || value > 140);
    }

    public static boolean isDiastolicBpAbnormal(Integer value) {
        return value != null && (value < 60 || value > 90);
    }

    public static boolean hasAnyMetricValue(HealthMetric metric) {
        return metric.getWeightKg() != null
                || metric.getWaistCm() != null
                || metric.getSystolicBp() != null
                || metric.getDiastolicBp() != null
                || metric.getFastingGlucose() != null
                || metric.getPostprandialGlucose() != null
                || metric.getHba1c() != null
                || metric.getDietStatus() != null
                || metric.getExerciseStatus() != null;
    }

    public static String buildSummary(HealthMetric metric) {
        StringBuilder summary = new StringBuilder();
        if (metric.getWeightKg() != null) {
            summary.append("体重").append(metric.getWeightKg()).append("kg");
        }
        if (metric.getFastingGlucose() != null) {
            if (!summary.isEmpty()) {
                summary.append("，");
            }
            summary.append("空腹血糖").append(metric.getFastingGlucose()).append("mmol/L");
        }
        if (metric.getSystolicBp() != null && metric.getDiastolicBp() != null) {
            if (!summary.isEmpty()) {
                summary.append("，");
            }
            summary.append("血压").append(metric.getSystolicBp()).append("/").append(metric.getDiastolicBp());
        }
        return summary.isEmpty() ? null : summary.toString();
    }

    public static BigDecimal calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null || heightCm.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal heightM = heightCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }
}
