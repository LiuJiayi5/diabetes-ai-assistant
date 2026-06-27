package com.diabetes.assistant.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    private String account;
    private String email;
    @JsonProperty("email_code")
    private String emailCode;

    @JsonProperty("new_password")
    private String newPassword;
}
