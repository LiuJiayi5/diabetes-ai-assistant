package com.diabetes.assistant.modules.aichat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatMessageRequest {

    @JsonProperty("session_id")
    private Integer sessionId;

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("expert_id")
    private Integer expertId;

    @NotBlank(message = "Message is required")
    private String message;
}
