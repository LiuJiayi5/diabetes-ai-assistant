package com.diabetes.assistant.modules.aichat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AiExpertSaveRequest {

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
}
