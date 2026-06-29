package com.diabetes.assistant.modules.aichat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiExpertResponse {

    @JsonProperty("expert_id")
    private Integer expertId;

    @JsonProperty("expert_name")
    private String expertName;

    private String title;
    private String department;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    private String specialty;
    private String persona;

    @JsonProperty("opening_message")
    private String openingMessage;

    @JsonProperty("sort_order")
    private Integer sortOrder;

    private String status;

    @JsonProperty("session_count")
    private Long sessionCount;

    @JsonProperty("create_time")
    private LocalDateTime createTime;

    @JsonProperty("update_time")
    private LocalDateTime updateTime;
}
