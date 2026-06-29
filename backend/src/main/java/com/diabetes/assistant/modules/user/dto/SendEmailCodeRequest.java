package com.diabetes.assistant.modules.user.dto;

import lombok.Data;

@Data
public class SendEmailCodeRequest {

    private String email;
    private String purpose;
}
