package com.diabetes.assistant.modules.user.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SendEmailCodeResponse {

    private String email;
    private String purpose;

    @JsonProperty("expires_in_seconds")
    private Integer expiresInSeconds;

    @JsonProperty("debug_code")
    private String debugCode;
}
