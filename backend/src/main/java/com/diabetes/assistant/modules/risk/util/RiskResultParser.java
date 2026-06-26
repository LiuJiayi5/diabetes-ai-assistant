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

        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawResponse);
            JsonNode payload = extractPayloadNode(root);
            ParsedRiskResult result = OBJECT_MAPPER.convertValue(payload, ParsedRiskResult.class);
            if (result.getMainRiskFactors() == null) {
                result.setMainRiskFactors(Collections.emptyList());
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Dify 返回格式异常", exception);
        }
    }

    private static JsonNode extractPayloadNode(JsonNode root) {
        if (root.has("risk_level") || root.has("riskLevel")) {
            return root;
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
                if (outputs.has("result")) {
                    JsonNode result = outputs.get("result");
                    return result.isTextual() ? readNode(result.asText()) : result;
                }
                return outputs;
            }
        }
        return root;
    }

    private static JsonNode readNode(String text) {
        String normalized = normalizeModelText(text);
        try {
            return OBJECT_MAPPER.readTree(normalized);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Dify outputs 不是合法 JSON", exception);
        }
    }

    private static String normalizeModelText(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = stripThinkBlock(text);
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
            int fenceEnd = cleaned.lastIndexOf("```");
            if (fenceEnd >= 0) {
                cleaned = cleaned.substring(0, fenceEnd).trim();
            }
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private static String stripThinkBlock(String text) {
        String openTag = "<" + "think" + ">";
        String closeTag = "</" + "think" + ">";
        int openIndex = text.indexOf(openTag);
        while (openIndex >= 0) {
            int closeIndex = text.indexOf(closeTag, openIndex);
            if (closeIndex < 0) {
                break;
            }
            text = text.substring(0, openIndex) + text.substring(closeIndex + closeTag.length());
            openIndex = text.indexOf(openTag);
        }
        return text.trim();
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

        @SuppressWarnings("unchecked")
        public void setMainRiskFactors(Object mainRiskFactors) {
            this.mainRiskFactors = toStringList(mainRiskFactors);
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
