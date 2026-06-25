package com.diabetes.assistant.modules.dify.client;

import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.modules.dify.config.DifyProperties;
import com.diabetes.assistant.modules.dify.dto.DifyWorkflowResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DifyClient {

    private final DifyProperties difyProperties;
    private final RestClient.Builder restClientBuilder;

    @SuppressWarnings("unchecked")
    public DifyWorkflowResult runWorkflow(String apiKey, Map<String, Object> inputs, String user) {
        if (!StringUtils.hasText(apiKey) || apiKey.startsWith("your_")) {
            throw new BusinessException(502, "Dify API Key 未配置");
        }
        if (!StringUtils.hasText(difyProperties.getBaseUrl())) {
            throw new BusinessException(502, "Dify base-url 未配置");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", user);

        try {
            String workflowUrl = normalizeBaseUrl(difyProperties.getBaseUrl()) + "/workflows/run";
            Map<?, ?> response = restClientBuilder
                    .build()
                    .post()
                    .uri(workflowUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Object data = response == null ? null : response.get("data");
            Object outputs = data instanceof Map<?, ?> dataMap ? dataMap.get("outputs") : response == null ? null : response.get("outputs");
            if (!(outputs instanceof Map<?, ?> outputMap)) {
                throw new BusinessException(502, "Dify 返回格式异常：缺少 outputs");
            }
            return new DifyWorkflowResult((Map<String, Object>) outputMap);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new BusinessException(502, "Dify workflow HTTP " + exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw new BusinessException(502, "Dify 工作流调用失败");
        }
    }

    public String sendChatMessage(String apiKey, String query, String conversationId, Map<String, Object> inputs, String user) {
        throw new BusinessException(502, "Dify chat client 尚未实现");
    }

    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
