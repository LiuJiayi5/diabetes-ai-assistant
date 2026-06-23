package com.diabetes.assistant.modules.aichat.service.impl;

import com.diabetes.assistant.modules.aichat.service.AiChatService;
import org.springframework.stereotype.Service;

@Service
public class AiChatServiceImpl implements AiChatService {

    @Override
    public String entry() {
        return "AI 医生咨询模块功能开发中";
    }
}
