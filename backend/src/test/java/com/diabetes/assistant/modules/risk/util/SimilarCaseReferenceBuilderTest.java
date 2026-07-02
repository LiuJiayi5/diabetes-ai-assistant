package com.diabetes.assistant.modules.risk.util;

import com.diabetes.assistant.modules.healthmetric.contract.dto.HealthMetricDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimilarCaseReferenceBuilderTest {

    @Test
    void buildInterventionSummary_combinesPlanAndCheckin() {
        String summary = SimilarCaseReferenceBuilder.buildInterventionSummary(
                "14天稳糖计划",
                "慢走、少盐、控制晚餐主食",
                new BigDecimal("86.4"));

        assertTrue(summary.contains("14天稳糖计划"));
        assertTrue(summary.contains("慢走、少盐"));
        assertTrue(summary.contains("86%"));
    }

    @Test
    void buildInterventionSummary_returnsFallbackWhenEmpty() {
        assertEquals("暂无生活方案或打卡记录",
                SimilarCaseReferenceBuilder.buildInterventionSummary(null, null, null));
    }

    @Test
    void buildOutcomeSummary_describesMetricTrend() {
        HealthMetricDTO earlier = metric(LocalDate.of(2026, 6, 1), "7.1", "82.0");
        HealthMetricDTO latest = metric(LocalDate.of(2026, 6, 20), "6.5", "81.0");

        String summary = SimilarCaseReferenceBuilder.buildOutcomeSummary(List.of(latest, earlier));

        assertTrue(summary.contains("空腹血糖由 7.1 降至 6.5 mmol/L"));
        assertTrue(summary.contains("体重由 82.0 降至 81.0 kg"));
    }

    @Test
    void buildOutcomeSummary_handlesSingleRecord() {
        assertEquals("仅 1 次指标记录，暂无可对比趋势",
                SimilarCaseReferenceBuilder.buildOutcomeSummary(List.of(metric(LocalDate.now(), "6.2", "80.0"))));
    }

    @Test
    void buildReferenceSummary_joinsUsefulParts() {
        String summary = SimilarCaseReferenceBuilder.buildReferenceSummary(
                "方案《稳糖计划》；慢走",
                "空腹血糖由 7.1 降至 6.5 mmol/L（对比最早与最近记录）");

        assertTrue(summary.contains("稳糖计划"));
        assertTrue(summary.contains("空腹血糖"));
    }

    @Test
    void buildReferenceSummary_returnsNullWhenNoUsefulData() {
        assertNull(SimilarCaseReferenceBuilder.buildReferenceSummary(
                "暂无生活方案或打卡记录",
                "暂无足够指标历史"));
    }

    private static HealthMetricDTO metric(LocalDate date, String glucose, String weight) {
        HealthMetricDTO metric = new HealthMetricDTO();
        metric.setRecordedAt(date);
        metric.setFastingGlucose(new BigDecimal(glucose));
        metric.setWeightKg(new BigDecimal(weight));
        return metric;
    }
}
