package com.diabetes.assistant.modules.aichat.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAiChatLogResponse {

    @JsonProperty("message_id")
    private Integer messageId;

    @JsonProperty("session_id")
    private Integer sessionId;

    @JsonProperty("user_id")
    private Integer userId;

    private String username;

    @JsonProperty("session_title")
    private String sessionTitle;

    @JsonProperty("user_message")
    private String userMessage;

    @JsonProperty("ai_response")
    private String aiResponse;

    @JsonProperty("context_summary")
    private String contextSummary;

    @JsonProperty("call_status")
    private String callStatus;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("create_time")
    private LocalDateTime createTime;
}
