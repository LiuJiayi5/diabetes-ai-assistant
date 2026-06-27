package com.diabetes.assistant.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RegisterRequest {

    private String username;
    private String password;
    private String phone;
    private String email;
    @JsonProperty("email_code")
    private String emailCode;
    private String avatar;
}
