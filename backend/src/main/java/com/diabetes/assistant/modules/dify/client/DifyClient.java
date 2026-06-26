package com.diabetes.assistant.modules.dify.client;

import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.modules.dify.config.DifyProperties;
import com.diabetes.assistant.modules.dify.dto.DifyWorkflowResult;
import com.fasterxml.jackson.core.JsonProcessingException;
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
            Object outputs = data instanceof Map<?, ?> dataMap
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
            throw new BusinessException(502, "Dify workflow HTTP " + exception.getStatusCode().value());
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
        body.put("response_mode", "blocking");
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
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return responseBody == null ? "" : responseBody;
        } catch (RestClientResponseException exception) {
            throw new BusinessException(502, "Dify chat HTTP " + exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw new BusinessException(502, "Dify chat call failed");
        }
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
        outputs.put("analysis_result", toJson(analysis));
        outputs.put("completion_rate", completionRate);
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
        String answer = "Dev mock AI doctor response. Configure Dify after DSL deployment for real output.";
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("answer", answer);
        response.put("conversation_id", StringUtils.hasText(conversationId) ? conversationId : "dev-ai-doctor-conversation");
        response.put("message_id", "dev-ai-doctor-message");
        response.put("context_received", inputs != null && !inputs.isEmpty());
        return toJson(response);
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
