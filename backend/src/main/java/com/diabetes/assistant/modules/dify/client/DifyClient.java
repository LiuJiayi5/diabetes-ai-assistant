package com.diabetes.assistant.modules.dify.client;

import com.diabetes.assistant.modules.dify.config.DifyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DifyClient {

    private final DifyProperties difyProperties;
    private final RestClient restClient = RestClient.create();

    public String runWorkflow(String apiKey, Map<String, Object> inputs, String user) {
        String url = normalizeBaseUrl(difyProperties.getBaseUrl()) + "/workflows/run";
        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", user);

        try {
            String responseBody = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return responseBody == null ? "" : responseBody;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("Dify API 调用失败: " + exception.getResponseBodyAsString(), exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Dify API 调用失败: " + exception.getMessage(), exception);
        }
    }

    public String sendChatMessage(String apiKey, String query, String conversationId,
                                  Map<String, Object> inputs, String user) {
        String url = normalizeBaseUrl(difyProperties.getBaseUrl()) + "/chat-messages";
        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs == null ? Map.of() : inputs);
        body.put("query", query);
        body.put("response_mode", "blocking");
        body.put("user", user);
        if (conversationId != null && !conversationId.isBlank()) {
            body.put("conversation_id", conversationId);
        }

        try {
            String responseBody = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return responseBody == null ? "" : responseBody;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("Dify API 调用失败: " + exception.getResponseBodyAsString(), exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Dify API 调用失败: " + exception.getMessage(), exception);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
