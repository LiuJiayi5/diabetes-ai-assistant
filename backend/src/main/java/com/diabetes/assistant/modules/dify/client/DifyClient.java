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
            return "{\"answer\":\"Dify ai doctor dev mock response\"}";
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", inputs == null ? Map.of() : inputs);
        body.put("query", query);
        body.put("response_mode", "blocking");
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
            return response.body();
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

    private String buildMockWorkflowResponse(Map<String, Object> inputs) {
        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("completion_rate", inputs.getOrDefault("completion_rate", 0));
        analysis.put("diet_summary", "Dev mock: diet check-in analysis is waiting for a real Dify API key.");
        analysis.put("exercise_summary", "Dev mock: exercise check-in analysis is waiting for a real Dify API key.");
        analysis.put("habit_score", 70);
        analysis.put("life_evaluation", "Dev mock: current lifestyle is available for local integration testing.");
        analysis.put("main_problems", java.util.List.of("Dify API key is not configured", "This is a local mock result"));
        analysis.put("improvement_suggestions", java.util.List.of(
                "Configure dify.checkin-analysis-api-key",
                "Verify the workflow output JSON fields",
                "Retest POST /api/ai/checkin-analysis"));
        analysis.put("next_focus", "Replace dev mock with the real checkin_behavior_analysis_workflow call.");
        analysis.put("summary", "Dev mock result generated because the Dify API key is missing or still uses the placeholder value.");

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
}
