package com.diabetes.assistant.modules.risk.util;

import com.diabetes.assistant.modules.healthmetric.contract.dto.HealthMetricDTO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SimilarCaseReferenceBuilder {

    private static final int PLAN_SUMMARY_MAX_LENGTH = 80;
    private static final BigDecimal MIN_CHANGE = new BigDecimal("0.05");

    private SimilarCaseReferenceBuilder() {
    }

    public static String buildInterventionSummary(String planTitle, String planSummary, BigDecimal checkinRate) {
        List<String> parts = new ArrayList<>();

        if (StringUtils.hasText(planTitle)) {
            parts.add("方案《" + planTitle.trim() + "》");
        }
        if (StringUtils.hasText(planSummary)) {
            String summary = planSummary.trim();
            if (summary.length() > PLAN_SUMMARY_MAX_LENGTH) {
                summary = summary.substring(0, PLAN_SUMMARY_MAX_LENGTH) + "…";
            }
            parts.add(summary);
        }
        if (checkinRate != null) {
            parts.add("近7天打卡完成率 " + checkinRate.setScale(0, RoundingMode.HALF_UP) + "%");
        }

        return parts.isEmpty() ? "暂无生活方案或打卡记录" : String.join("；", parts);
    }

    public static String buildOutcomeSummary(List<HealthMetricDTO> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return "暂无足够指标历史";
        }
        if (metrics.size() == 1) {
            return "仅 1 次指标记录，暂无可对比趋势";
        }

        List<HealthMetricDTO> sorted = metrics.stream()
                .sorted(Comparator.comparing(HealthMetricDTO::getRecordedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        HealthMetricDTO earliest = sorted.get(0);
        HealthMetricDTO latest = sorted.get(sorted.size() - 1);

        List<String> changes = new ArrayList<>();
        appendMetricChange(changes, "空腹血糖", earliest.getFastingGlucose(), latest.getFastingGlucose(), " mmol/L");
        appendMetricChange(changes, "餐后血糖", earliest.getPostprandialGlucose(), latest.getPostprandialGlucose(), " mmol/L");
        appendMetricChange(changes, "体重", earliest.getWeightKg(), latest.getWeightKg(), " kg");
        appendMetricChange(changes, "腰围", earliest.getWaistCm(), latest.getWaistCm(), " cm");

        if (changes.isEmpty()) {
            return "指标变化不明显（对比最早与最近记录）";
        }
        return String.join("，", changes) + "（对比最早与最近记录）";
    }

    public static String buildReferenceSummary(String interventionSummary, String outcomeSummary) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(interventionSummary)
                && !"暂无生活方案或打卡记录".equals(interventionSummary)) {
            parts.add(interventionSummary);
        }
        if (StringUtils.hasText(outcomeSummary)
                && !outcomeSummary.startsWith("暂无")
                && !outcomeSummary.startsWith("仅 1 次")) {
            parts.add(outcomeSummary);
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join("；", parts);
    }

    private static void appendMetricChange(List<String> changes, String label,
                                           BigDecimal from, BigDecimal to, String unit) {
        if (from == null || to == null) {
            return;
        }
        if (from.subtract(to).abs().compareTo(MIN_CHANGE) < 0) {
            return;
        }
        String fromText = RiskSimilarityUtil.round(from).toPlainString();
        String toText = RiskSimilarityUtil.round(to).toPlainString();
        if (to.compareTo(from) < 0) {
            changes.add(label + "由 " + fromText + " 降至 " + toText + unit);
        } else {
            changes.add(label + "由 " + fromText + " 升至 " + toText + unit);
        }
    }
}
