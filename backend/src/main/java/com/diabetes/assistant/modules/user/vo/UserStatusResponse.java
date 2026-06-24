package com.diabetes.assistant.modules.user.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserStatusResponse {

    @JsonProperty("user_id")
    private Integer userId;
    private String status;
    @JsonProperty("update_time")
    private String updateTime;
}
