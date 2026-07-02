package com.diabetes.assistant.modules.healthmetric.util;

import com.diabetes.assistant.modules.healthmetric.entity.HealthMetric;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricAbnormalUtilsTest {

    @Test
    void calculateBmi_returnsNullWhenInputMissing() {
        assertNull(MetricAbnormalUtils.calculateBmi(null, new BigDecimal("70")));
        assertNull(MetricAbnormalUtils.calculateBmi(new BigDecimal("170"), null));
        assertNull(MetricAbnormalUtils.calculateBmi(BigDecimal.ZERO, new BigDecimal("70")));
    }

    @Test
    void calculateBmi_computesExpectedValue() {
        BigDecimal bmi = MetricAbnormalUtils.calculateBmi(new BigDecimal("170"), new BigDecimal("68"));
        assertEquals(new BigDecimal("23.5"), bmi);
    }

    @Test
    void isFastingGlucoseAbnormal_detectsOutOfRange() {
        assertFalse(MetricAbnormalUtils.isFastingGlucoseAbnormal(null));
        assertFalse(MetricAbnormalUtils.isFastingGlucoseAbnormal(new BigDecimal("5.6")));
        assertTrue(MetricAbnormalUtils.isFastingGlucoseAbnormal(new BigDecimal("7.1")));
        assertTrue(MetricAbnormalUtils.isFastingGlucoseAbnormal(new BigDecimal("3.5")));
    }

    @Test
    void isSystolicBpAbnormal_detectsOutOfRange() {
        assertFalse(MetricAbnormalUtils.isSystolicBpAbnormal(120));
        assertTrue(MetricAbnormalUtils.isSystolicBpAbnormal(150));
        assertTrue(MetricAbnormalUtils.isSystolicBpAbnormal(80));
    }

    @Test
    void hasAnyMetricValue_requiresAtLeastOneField() {
        HealthMetric empty = new HealthMetric();
        assertFalse(MetricAbnormalUtils.hasAnyMetricValue(empty));

        HealthMetric metric = new HealthMetric();
        metric.setWeightKg(new BigDecimal("70"));
        assertTrue(MetricAbnormalUtils.hasAnyMetricValue(metric));
    }

    @Test
    void buildSummary_joinsAvailableFields() {
        HealthMetric metric = new HealthMetric();
        metric.setWeightKg(new BigDecimal("82.5"));
        metric.setFastingGlucose(new BigDecimal("6.2"));
        metric.setSystolicBp(130);
        metric.setDiastolicBp(82);
        String summary = MetricAbnormalUtils.buildSummary(metric);
        assertTrue(summary.contains("体重82.5kg"));
        assertTrue(summary.contains("空腹血糖6.2mmol/L"));
        assertTrue(summary.contains("血压130/82"));
    }
}
