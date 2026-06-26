package com.diabetes.assistant.modules.aichat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatSessionResponse {

    @JsonProperty("session_id")
    private Integer sessionId;

    @JsonProperty("session_title")
    private String sessionTitle;

    @JsonProperty("dify_conversation_id")
    private String difyConversationId;

    private String status;

    @JsonProperty("last_message_time")
    private LocalDateTime lastMessageTime;

    @JsonProperty("create_time")
    private LocalDateTime createTime;

    @JsonProperty("update_time")
    private LocalDateTime updateTime;
}
