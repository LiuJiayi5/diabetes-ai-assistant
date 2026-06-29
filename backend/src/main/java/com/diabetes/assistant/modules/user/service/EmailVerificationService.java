package com.diabetes.assistant.modules.user.service;

import com.diabetes.assistant.modules.user.dto.SendEmailCodeRequest;
import com.diabetes.assistant.modules.user.vo.SendEmailCodeResponse;

public interface EmailVerificationService {

    String PURPOSE_REGISTER = "register";
    String PURPOSE_RESET_PASSWORD = "reset_password";

    SendEmailCodeResponse sendCode(SendEmailCodeRequest request);

    void verifyAndConsume(String email, String purpose, String code);
}
