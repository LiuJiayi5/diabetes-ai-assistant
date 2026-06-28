package com.diabetes.assistant.modules.risk.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class RiskResultParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private RiskResultParser() {
    }

    public static ParsedRiskResult parse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException("Dify 返回为空");
        }

        String readableText = null;
        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawResponse);
            readableText = extractReadableText(root);
            JsonNode payload = extractPayloadNode(root);
            ParsedRiskResult result = OBJECT_MAPPER.convertValue(payload, ParsedRiskResult.class);
            if (result.getMainRiskFactors() == null) {
                result.setMainRiskFactors(Collections.emptyList());
            }
            result.setOriginalText(readableText);
            return result;
        } catch (Exception exception) {
            return fromPlainText(readableText == null ? rawResponse : readableText, exception);
        }
    }

    private static JsonNode extractPayloadNode(JsonNode root) {
        if (root.has("risk_level") || root.has("riskLevel")) {
            return root;
        }
        if (root.has("risk_result")) {
            JsonNode result = root.get("risk_result");
            return result.isTextual() ? readNode(result.asText()) : result;
        }
        if (root.has("text")) {
            return readNode(root.get("text").asText());
        }
        if (root.has("result")) {
            JsonNode result = root.get("result");
            return result.isTextual() ? readNode(result.asText()) : result;
        }
        if (root.has("data")) {
            JsonNode data = root.get("data");
            if (data.has("outputs")) {
                JsonNode outputs = data.get("outputs");
                if (outputs.isTextual()) {
                    return readNode(outputs.asText());
                }
                if (outputs.has("text")) {
                    return readNode(outputs.get("text").asText());
                }
                if (outputs.has("risk_result")) {
                    JsonNode result = outputs.get("risk_result");
                    return result.isTextual() ? readNode(result.asText()) : result;
                }
                if (outputs.has("result")) {
                    JsonNode result = outputs.get("result");
                    return result.isTextual() ? readNode(result.asText()) : result;
                }
                return outputs;
            }
        }
        return root;
    }

    private static String extractReadableText(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isTextual()) {
            return root.asText();
        }
        JsonNode outputs = root.path("data").path("outputs");
        if (!outputs.isMissingNode()) {
            String text = firstText(outputs, "risk_result", "text", "result", "answer", "output");
            if (text != null) {
                return text;
            }
        }
        return firstText(root, "risk_result", "text", "result", "answer", "output");
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isTextual()) {
                return value.asText();
            }
            if (value.isObject() || value.isArray()) {
                return value.toString();
            }
        }
        return null;
    }

    private static JsonNode readNode(String text) {
        String normalized = stripReasoningText(text);
        Exception lastException = null;
        for (String candidate : jsonCandidates(normalized)) {
            try {
                return OBJECT_MAPPER.readTree(candidate);
            } catch (Exception exception) {
                lastException = exception;
            }
        }
        throw new IllegalArgumentException("Dify outputs 不是合法 JSON", lastException);
    }

    private static String stripReasoningText(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int thinkEnd = trimmed.lastIndexOf("</think>");
        if (thinkEnd >= 0) {
            trimmed = trimmed.substring(thinkEnd + "</think>".length()).trim();
        }
        return trimmed;
    }

    private static List<String> jsonCandidates(String text) {
        List<String> candidates = new ArrayList<>();
        String trimmed = text == null ? "" : text.trim();
        collectJsonObjects(trimmed, candidates);
        if (candidates.isEmpty() && !trimmed.isEmpty()) {
            candidates.add(trimmed);
        }
        Collections.reverse(candidates);
        return candidates;
    }

    private static void collectJsonObjects(String trimmed, List<String> candidates) {
        int start = trimmed.indexOf('{');
        while (start >= 0 && start < trimmed.length()) {
            int end = findMatchingBrace(trimmed, start);
            if (end > start) {
                candidates.add(trimmed.substring(start, end + 1));
            }
            start = trimmed.indexOf('{', start + 1);
        }
    }

    private static int findMatchingBrace(String text, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static ParsedRiskResult fromPlainText(String text, Exception cause) {
        String normalized = normalizeText(text);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Dify outputs 不是合法 JSON", cause);
        }

        ParsedRiskResult result = new ParsedRiskResult();
        result.setRiskLevel(inferRiskLevel(normalized));
        result.setRiskScore(inferRiskScore(result.getRiskLevel()));
        result.setDiabetesTypeTendency(inferTypeTendency(normalized));
        result.setMainRiskFactors(extractRiskFactors(normalized));
        result.setIndicatorAnalysis(normalized);
        result.setHealthAdvice(extractSection(normalized, "建议", "就医", "提醒", "总结"));
        result.setMedicalWarning(extractSection(normalized, "就医", "复查", "提醒"));
        result.setSummary(firstSentence(normalized));
        result.setOriginalText(normalized);
        result.setFormatWarning("Dify 输出不是结构化 JSON，已按文本报告兼容解析");
        return result;
    }

    private static String normalizeText(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }
        return normalized.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String inferRiskLevel(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("high") || text.contains("高风险") || text.contains("风险较高") || text.contains("重度")) {
            return "high";
        }
        if (lower.contains("low") || text.contains("低风险") || text.contains("风险较低")) {
            return "low";
        }
        return "medium";
    }

    private static int inferRiskScore(String level) {
        return switch (level == null ? "" : level) {
            case "high" -> 82;
            case "low" -> 28;
            default -> 58;
        };
    }

    private static String inferTypeTendency(String text) {
        if (text.contains("1型") || text.toLowerCase().contains("type 1")) {
            return "1型倾向需结合线下检查确认";
        }
        if (text.contains("妊娠")) {
            return "妊娠期血糖异常倾向需结合线下检查确认";
        }
        return "2型倾向需结合线下检查确认";
    }

    private static List<String> extractRiskFactors(String text) {
        List<String> factors = new ArrayList<>();
        addFactorIfPresent(factors, text, "空腹血糖", "空腹血糖异常");
        addFactorIfPresent(factors, text, "餐后血糖", "餐后血糖异常");
        addFactorIfPresent(factors, text, "糖化血红蛋白", "糖化血红蛋白异常");
        addFactorIfPresent(factors, text, "BMI", "BMI偏高");
        addFactorIfPresent(factors, text, "体重", "体重管理风险");
        addFactorIfPresent(factors, text, "血压", "血压异常");
        addFactorIfPresent(factors, text, "家族", "糖尿病家族史");
        if (factors.isEmpty()) {
            factors.add("Dify文本报告未拆分风险因素，请结合完整分析查看");
        }
        return factors;
    }

    private static void addFactorIfPresent(List<String> factors, String text, String keyword, String factor) {
        if (text.contains(keyword) && !factors.contains(factor)) {
            factors.add(factor);
        }
    }

    private static String extractSection(String text, String... keywords) {
        for (String keyword : keywords) {
            int index = text.indexOf(keyword);
            if (index >= 0) {
                return text.substring(index).trim();
            }
        }
        return "";
    }

    private static String firstSentence(String text) {
        String compact = text.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 120) {
            return compact;
        }
        int end = compact.indexOf('。');
        if (end > 20 && end < 120) {
            return compact.substring(0, end + 1);
        }
        return compact.substring(0, 120) + "…";
    }

    @Data
    public static class ParsedRiskResult {
        private String riskLevel;
        private Integer riskScore;
        private String diabetesTypeTendency;
        private List<String> mainRiskFactors;
        private String indicatorAnalysis;
        private String healthAdvice;
        private String medicalWarning;
        private String summary;
        private List<String> referenceSources;
        private String originalText;
        private String formatWarning;

        @SuppressWarnings("unchecked")
        public void setMainRiskFactors(Object mainRiskFactors) {
            this.mainRiskFactors = toStringList(mainRiskFactors);
        }

        @SuppressWarnings("unchecked")
        public void setReferenceSources(Object referenceSources) {
            this.referenceSources = toStringList(referenceSources);
        }

        private List<String> toStringList(Object value) {
            if (value == null) {
                return new ArrayList<>();
            }
            if (value instanceof List<?> list) {
                return list.stream().map(String::valueOf).toList();
            }
            if (value instanceof String text && text.startsWith("[")) {
                try {
                    return OBJECT_MAPPER.readValue(text, new TypeReference<List<String>>() {
                    });
                } catch (Exception ignored) {
                    return List.of(text);
                }
            }
            return List.of(String.valueOf(value));
        }
    }

    public static String toJson(Map<String, Object> map) {
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (Exception exception) {
            throw new IllegalStateException("JSON 序列化失败", exception);
        }
    }
}
