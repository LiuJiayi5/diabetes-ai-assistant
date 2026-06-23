package com.diabetes.assistant.modules.aichat.contract.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatSessionSummaryDTO {

    private Integer sessionId;
    private Integer userId;
    private String sessionTitle;
    private String status;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createTime;
}
