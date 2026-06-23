package com.diabetes.assistant.modules.dify.client;

import com.diabetes.assistant.modules.dify.config.DifyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DifyClient {

    private final DifyProperties difyProperties;

    public String runWorkflow(String apiKey, Map<String, Object> inputs, String user) {
        // TODO: 使用 RestClient/WebClient 调用 {baseUrl}/workflows/run。
        return "TODO run Dify workflow via " + difyProperties.getBaseUrl();
    }

    public String sendChatMessage(String apiKey, String query, String conversationId, Map<String, Object> inputs, String user) {
        // TODO: 使用 RestClient/WebClient 调用 {baseUrl}/chat-messages。
        return "TODO send Dify chat message via " + difyProperties.getBaseUrl();
    }
}
