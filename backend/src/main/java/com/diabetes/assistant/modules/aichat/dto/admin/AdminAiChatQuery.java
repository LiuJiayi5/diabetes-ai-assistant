package com.diabetes.assistant.modules.aichat.dto.admin;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminAiChatQuery {

    private Integer page;
    private Integer pageSize;
    private Integer userId;
    private Integer expertId;
    private String userKeyword;
    private LocalDate startDate;
    private LocalDate endDate;
    private String keyword;
    private String callStatus;
}
