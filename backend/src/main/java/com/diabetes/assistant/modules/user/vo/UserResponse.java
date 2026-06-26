package com.diabetes.assistant.modules.user.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    @JsonProperty("user_id")
    private Integer userId;
    private String username;
    private String phone;
    private String email;
    private String avatar;
    private String role;
    private String status;
    @JsonProperty("last_login_time")
    private String lastLoginTime;
    @JsonProperty("create_time")
    private String createTime;
    @JsonProperty("update_time")
    private String updateTime;
}
