package com.diabetes.assistant.modules.aichat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatMessageResponse {

    @JsonProperty("session_id")
    private Integer sessionId;

    @JsonProperty("message_id")
    private Integer messageId;

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("expert_id")
    private Integer expertId;

    @JsonProperty("expert_name")
    private String expertName;

    @JsonProperty("expert_avatar_url")
    private String expertAvatarUrl;

    @JsonProperty("user_message")
    private String userMessage;

    private String answer;

    @JsonProperty("context_summary")
    private String contextSummary;

    @JsonProperty("call_status")
    private String callStatus;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("create_time")
    private LocalDateTime createTime;
}
