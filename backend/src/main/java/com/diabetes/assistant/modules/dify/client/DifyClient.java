package com.diabetes.assistant.modules.dify.client;

import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.modules.dify.config.DifyProperties;
import com.diabetes.assistant.modules.dify.dto.DifyWorkflowResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DifyClient {

    private final DifyProperties difyProperties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public DifyWorkflowResult runWorkflow(String apiKey, Map<String, Object> inputs, String user) {
        if (useDevMock(apiKey)) {
            return new DifyWorkflowResult(buildMockWorkflowOutputs(inputs));
        }
        if (!StringUtils.hasText(difyProperties.getBaseUrl())) {
            throw new BusinessException(502, "Dify base-url is not configured");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", inputs == null ? Map.of() : inputs);
        body.put("response_mode", "blocking");
        body.put("user", StringUtils.hasText(user) ? user : "dev-user");

        try {
            Map<?, ?> response = restClientBuilder
                    .build()
                    .post()
                    .uri(normalizeBaseUrl() + "/workflows/run")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Object data = response == null ? null : response.get("data");
            Map<?, ?> dataMap = data instanceof Map<?, ?> map ? map : null;
            if (dataMap != null && "failed".equals(String.valueOf(dataMap.get("status")))) {
                throw new BusinessException(502, buildWorkflowFailureMessage(dataMap));
            }
            Object outputs = dataMap != null
                    ? dataMap.get("outputs")
                    : response == null ? null : response.get("outputs");
            if (!(outputs instanceof Map<?, ?> outputMap)) {
                throw new BusinessException(502, "Dify response is missing outputs");
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            outputMap.forEach((key, value) -> normalized.put(String.valueOf(key), value));
            return new DifyWorkflowResult(normalized);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new BusinessException(502, buildHttpErrorMessage("Dify workflow HTTP ", exception));
        } catch (RestClientException exception) {
            throw new BusinessException(502, "Dify workflow call failed");
        }
    }

    public String sendChatMessage(String apiKey, String query, String conversationId,
                                  Map<String, Object> inputs, String user) {
        if (useDevMock(apiKey)) {
            return buildMockChatResponse(query, conversationId, inputs);
        }
        if (!StringUtils.hasText(difyProperties.getBaseUrl())) {
            throw new BusinessException(502, "Dify base-url is not configured");
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
            String responseBody = restClientBuilder
                    .build()
                    .post()
                    .uri(normalizeBaseUrl() + "/chat-messages")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseStreamingChatResponse(responseBody);
        } catch (RestClientResponseException exception) {
            throw new BusinessException(502, buildHttpErrorMessage("Dify chat HTTP ", exception));
        } catch (RestClientException exception) {
            throw new BusinessException(502, "Dify chat call failed");
        }
    }

    private String parseStreamingChatResponse(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "{}";
        }
        StringBuilder answer = new StringBuilder();
        String conversationId = "";
        String messageId = "";
        for (String line : responseBody.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("data:")) {
                continue;
            }
            String payload = trimmed.substring("data:".length()).trim();
            if (!StringUtils.hasText(payload) || "[DONE]".equals(payload)) {
                continue;
            }
            try {
                JsonNode event = objectMapper.readTree(payload);
                String eventName = event.path("event").asText();
                if (!StringUtils.hasText(conversationId)) {
                    conversationId = event.path("conversation_id").asText("");
                }
                if (!StringUtils.hasText(messageId)) {
                    messageId = event.path("message_id").asText(event.path("id").asText(""));
                }
                if ("message".equals(eventName) || "agent_message".equals(eventName)) {
                    answer.append(event.path("answer").asText(""));
                }
            } catch (JsonProcessingException ignored) {
                // Ignore malformed SSE housekeeping lines and keep parsing useful chunks.
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("answer", answer.toString());
        response.put("conversation_id", conversationId);
        response.put("message_id", messageId);
        return toJson(response);
    }

    private String buildHttpErrorMessage(String prefix, RestClientResponseException exception) {
        String message = prefix + exception.getStatusCode().value();
        String body = exception.getResponseBodyAsString();
        if (!StringUtils.hasText(body)) {
            return message;
        }
        String compactBody = body.replaceAll("\\s+", " ").trim();
        if (compactBody.length() > 300) {
            compactBody = compactBody.substring(0, 300);
        }
        return message + ": " + compactBody;
    }

    private String buildWorkflowFailureMessage(Map<?, ?> dataMap) {
        Object error = dataMap.get("error");
        if (error == null || !StringUtils.hasText(String.valueOf(error))) {
            return "Dify workflow run failed";
        }
        String compactError = String.valueOf(error).replaceAll("\\s+", " ").trim();
        if (compactError.length() > 500) {
            compactError = compactError.substring(0, 500);
        }
        return "Dify workflow run failed: " + compactError;
    }

    private String normalizeBaseUrl() {
        String baseUrl = difyProperties.getBaseUrl();
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
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

    private Map<String, Object> buildMockWorkflowOutputs(Map<String, Object> inputs) {
        Map<String, Object> safeInputs = inputs == null ? Map.of() : inputs;
        if (safeInputs.containsKey("checkin_records") || safeInputs.containsKey("completion_rate")) {
            return buildMockCheckinOutputs(safeInputs);
        }
        if (safeInputs.containsKey("plan_goal") || safeInputs.containsKey("plan_days")) {
            return buildMockLifePlanOutputs(safeInputs);
        }
        return buildMockRiskOutputs();
    }

    private Map<String, Object> buildMockCheckinOutputs(Map<String, Object> inputs) {
        int totalDays = Math.max(1, toInt(inputs.get("total_days")));
        int dietCount = toInt(inputs.get("diet_completion_count"));
        int exerciseCount = toInt(inputs.get("exercise_completion_count"));
        double completionRate = toDouble(inputs.getOrDefault("completion_rate", 0));
        int habitScore = (int) Math.max(45, Math.min(90, Math.round(completionRate)));

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("completion_rate", completionRate);
        analysis.put("diet_summary", "Dev mock: diet check-ins completed " + dietCount + " times.");
        analysis.put("exercise_summary", "Dev mock: exercise check-ins completed " + exerciseCount + " times.");
        analysis.put("habit_score", habitScore);
        analysis.put("life_evaluation", "Dev mock for " + totalDays + " days. Configure Dify keys for real output.");
        analysis.put("main_problems", List.of("Dify key is not configured", "This is local integration mock data"));
        analysis.put("improvement_suggestions", List.of(
                "Configure Dify workflow key",
                "Verify workflow output JSON fields",
                "Use statistics page to compare completion rate"));
        analysis.put("next_focus", "Configure real Dify workflow and regenerate analysis.");
        analysis.put("summary", "Dev mock check-in analysis result.");

        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("success", true);
        outputs.put("analysis_result", toJson(analysis));
        outputs.put("completion_rate", completionRate);
        outputs.put("error_message", "");
        outputs.put("missing_fields", List.of());
        outputs.put("input_summary", "Dev mock check-in analysis output");
        return outputs;
    }

    private Map<String, Object> buildMockLifePlanOutputs(Map<String, Object> inputs) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("plan_title", "Dev mock life plan");
        plan.put("plan_goal", String.valueOf(inputs.getOrDefault("plan_goal", "daily glucose control")));
        plan.put("summary", "Dev mock life plan generated without a real Dify workflow.");
        plan.put("diet_plan", Map.of("principle", "Balanced meals with controlled staple food."));
        plan.put("exercise_plan", Map.of("principle", "Light post-meal walking and regular aerobic exercise."));
        plan.put("daily_schedule", List.of(Map.of("day", 1, "title", "Day 1", "reminder", "Record diet and activity.")));
        plan.put("health_tips", List.of("This mock plan is only for local integration testing."));
        plan.put("checkin_tasks", List.of(
                Map.of("task_type", "diet", "task_name", "Complete diet plan"),
                Map.of("task_type", "exercise", "task_name", "Complete post-meal walk")));

        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("success", true);
        outputs.put("plan_result", plan);
        outputs.put("input_summary", "Dev mock workflow output");
        return outputs;
    }

    private Map<String, Object> buildMockRiskOutputs() {
        Map<String, Object> risk = new LinkedHashMap<>();
        risk.put("risk_level", "medium");
        risk.put("risk_score", 65);
        risk.put("diabetes_type_tendency", "type 2 tendency");
        risk.put("main_risk_factors", List.of("fasting glucose is high", "BMI may be high"));
        risk.put("indicator_analysis", "Dev mock risk result for local integration.");
        risk.put("health_advice", "Improve diet, exercise regularly, and monitor glucose.");
        risk.put("medical_warning", "Consult an offline doctor if values are abnormal.");
        risk.put("summary", "Dev mock medium risk result.");
        return Map.of("risk_result", toJson(risk));
    }

    private String buildMockChatResponse(String query, String conversationId, Map<String, Object> inputs) {
        Map<String, Object> expert = inputs == null ? Map.of() : asMap(inputs.get("expert_identity"));
        if (System.currentTimeMillis() >= 0) {
            String expertName = String.valueOf(expert.getOrDefault("expert_name", "AI doctor assistant"));
            String title = String.valueOf(expert.getOrDefault("title", "Diabetes prevention assistant"));
            String department = String.valueOf(expert.getOrDefault("department", "Smart health consultation"));
            String specialty = String.valueOf(expert.getOrDefault("specialty", "diabetes health consultation"));
            String persona = String.valueOf(expert.getOrDefault("persona",
                    "I combine health profile, latest metrics, risk assessment, life plan, and check-in records to provide safe and actionable glucose-control advice."));
            String answer = "I am " + expertName + ", " + title + " from " + department
                    + ". My focus is " + specialty + ". " + persona
                    + " This is a local Dify mock response and the current expert identity was received.";
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("answer", answer);
            response.put("conversation_id", StringUtils.hasText(conversationId) ? conversationId : "dev-ai-doctor-conversation");
            response.put("message_id", "dev-ai-doctor-message");
            response.put("context_received", inputs != null && !inputs.isEmpty());
            return toJson(response);
        }
        String expertName = String.valueOf(expert.getOrDefault("expert_name", "AI医生助手"));
        String title = String.valueOf(expert.getOrDefault("title", "糖尿病预治智能助手"));
        String department = String.valueOf(expert.getOrDefault("department", "智能健康咨询"));
        String specialty = String.valueOf(expert.getOrDefault("specialty", "糖尿病健康咨询"));
        String answer = "我是" + expertName + "，" + title + "，来自" + department
                + "。我主要负责" + specialty
                + "。这是本地Dify模拟回复，已接收到当前专家身份和用户健康上下文。";
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("answer", answer);
        response.put("conversation_id", StringUtils.hasText(conversationId) ? conversationId : "dev-ai-doctor-conversation");
        response.put("message_id", "dev-ai-doctor-message");
        response.put("context_received", inputs != null && !inputs.isEmpty());
        return toJson(response);
    }

    private Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        map.forEach((key, item) -> normalized.put(String.valueOf(key), item));
        return normalized;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
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
