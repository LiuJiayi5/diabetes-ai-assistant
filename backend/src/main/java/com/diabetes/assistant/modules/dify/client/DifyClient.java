package com.diabetes.assistant.modules.dify.client;

import com.diabetes.assistant.modules.dify.config.DifyProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DifyClient {

    private final DifyProperties difyProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String runWorkflow(String apiKey, Map<String, Object> inputs, String user) {
        if (useDevMock(apiKey)) {
            return buildMockWorkflowResponse(inputs);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", StringUtils.hasText(user) ? user : "dev-user");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl() + "/workflows/run"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Dify workflow request failed: HTTP " + response.statusCode()
                        + " " + response.body());
            }
            return response.body();
        } catch (IOException exception) {
            throw new IllegalStateException("Dify workflow request failed: " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Dify workflow request interrupted", exception);
        }
    }

    public String sendChatMessage(String apiKey, String query, String conversationId, Map<String, Object> inputs, String user) {
        if (useDevMock(apiKey)) {
            return buildMockChatResponse(query, conversationId, inputs);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", normalizeChatInputs(inputs));
        body.put("query", query);
        body.put("response_mode", "streaming");
        body.put("user", StringUtils.hasText(user) ? user : "dev-user");
        if (StringUtils.hasText(conversationId)) {
            body.put("conversation_id", conversationId);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl() + "/chat-messages"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Dify chat request failed: HTTP " + response.statusCode()
                        + " " + response.body());
            }
            return parseChatStreamResponse(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Dify chat request failed: " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Dify chat request interrupted", exception);
        }
    }

    private String normalizeBaseUrl() {
        String baseUrl = difyProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Dify base-url is not configured");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private boolean useDevMock(String apiKey) {
        return !StringUtils.hasText(apiKey) || apiKey.startsWith("your_");
    }

    private Map<String, Object> normalizeChatInputs(Map<String, Object> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        inputs.forEach((key, value) -> normalized.put(key, toPromptInputText(value)));
        return normalized;
    }

    private String toPromptInputText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }

    private String parseChatStreamResponse(String body) {
        if (!StringUtils.hasText(body) || !body.contains("data:")) {
            return body;
        }

        StringBuilder answer = new StringBuilder();
        String conversationId = "";
        String messageId = "";

        for (String rawLine : body.split("\\R")) {
            String line = rawLine.trim();
            if (!line.startsWith("data:")) {
                continue;
            }
            String payload = line.substring("data:".length()).trim();
            if (!StringUtils.hasText(payload) || "[DONE]".equals(payload)) {
                continue;
            }
            try {
                var node = objectMapper.readTree(payload);
                String event = node.path("event").asText();
                if ("error".equals(event)) {
                    throw new IllegalStateException(node.path("message").asText("Dify chat stream returned error"));
                }
                String chunk = node.path("answer").asText("");
                if (StringUtils.hasText(chunk)) {
                    answer.append(chunk);
                }
                if (!StringUtils.hasText(conversationId)) {
                    conversationId = node.path("conversation_id").asText("");
                }
                if (!StringUtils.hasText(messageId)) {
                    messageId = node.path("message_id").asText("");
                    if (!StringUtils.hasText(messageId)) {
                        messageId = node.path("id").asText("");
                    }
                }
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Dify chat stream returned invalid event", exception);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("answer", answer.toString());
        response.put("conversation_id", conversationId);
        response.put("message_id", messageId);
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            return "{\"answer\":\"" + answer + "\"}";
        }
    }

    private String buildMockWorkflowResponse(Map<String, Object> inputs) {
        int completedCount = toInt(inputs.get("diet_completion_count")) + toInt(inputs.get("exercise_completion_count"));
        int totalDays = Math.max(1, toInt(inputs.get("total_days")));
        double completionRate = toDouble(inputs.getOrDefault("completion_rate", 0));
        int habitScore = (int) Math.max(45, Math.min(90, Math.round(completionRate)));

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("completion_rate", inputs.getOrDefault("completion_rate", 0));
        analysis.put("diet_summary", "当前 Dify API Key 未配置，以下为本地模拟分析。饮食打卡完成 "
                + toInt(inputs.get("diet_completion_count")) + " 次，请以真实工作流配置后的结果为准。");
        analysis.put("exercise_summary", "当前 Dify API Key 未配置，以下为本地模拟分析。运动打卡完成 "
                + toInt(inputs.get("exercise_completion_count")) + " 次，请以真实工作流配置后的结果为准。");
        analysis.put("habit_score", habitScore);
        analysis.put("life_evaluation", "本结果为本地开发模拟输出，不是真实 AI 分析。当前统计范围 "
                + totalDays + " 天，已完成任务 " + completedCount + " 项，完成率约 " + completionRate + "%。");
        analysis.put("main_problems", java.util.List.of("Dify API Key 未配置，当前不是真实 AI 输出", "本地模拟仅用于前后端联调展示"));
        analysis.put("improvement_suggestions", java.util.List.of(
                "配置 dify.checkin-analysis-api-key 后重新生成分析",
                "确认 checkin_behavior_analysis_workflow 输出 JSON 字段完整",
                "真实接入前，可先用统计页核对完成率口径"));
        analysis.put("next_focus", "配置真实 Dify 工作流后重新生成分析。");
        analysis.put("summary", "本地模拟结果：Dify API Key 缺失或仍为占位值。");

        Map<String, Object> outputs = new LinkedHashMap<>();
        try {
            outputs.put("analysis_result", objectMapper.writeValueAsString(analysis));
        } catch (JsonProcessingException exception) {
            outputs.put("analysis_result", "{}");
        }
        outputs.put("completion_rate", inputs.getOrDefault("completion_rate", 0));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "succeeded");
        data.put("outputs", outputs);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", data);
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            return "{\"data\":{\"status\":\"succeeded\",\"outputs\":{\"analysis_result\":\"{}\"}}}";
        }
    }

    private String buildMockChatResponse(String query, String conversationId, Map<String, Object> inputs) {
        String answer = "根据目前提供的信息，可以先从饮食、运动、监测和作息四个方面排查。"
                + "如果你是在询问最近打卡或分析结果，建议重点看完成率、饮食记录是否连续、运动是否稳定。"
                + "可以先尝试每天完成一次饮食记录和一次运动记录，连续 7 天后再看趋势。"
                + "如果出现明显不适或血糖读数异常，请尽快线下就医或复查。";
        String lowerQuery = StringUtils.hasText(query) ? query.toLowerCase() : "";
        if (lowerQuery.contains("completion") || lowerQuery.contains("完成率")) {
            answer = "完成率偏低通常说明饮食或运动任务还没有形成稳定习惯。"
                    + "建议先从最容易坚持的一项开始，比如饭后固定步行 15 到 30 分钟，"
                    + "同时连续记录 7 天饮食内容，再结合趋势调整。"
                    + "以上是一般健康建议，不能替代线下医生诊疗。";
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("answer", answer);
        response.put("conversation_id", StringUtils.hasText(conversationId) ? conversationId : "dev-ai-doctor-conversation");
        response.put("message_id", "dev-ai-doctor-message");
        response.put("safety_notice", "AI 建议仅供参考，不能替代线下医生诊疗。");
        response.put("context_received", inputs != null && !inputs.isEmpty());
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            return "{\"answer\":\"AI doctor development mock response.\",\"conversation_id\":\"dev-ai-doctor-conversation\"}";
        }
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
