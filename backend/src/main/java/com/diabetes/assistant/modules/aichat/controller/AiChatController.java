package com.diabetes.assistant.modules.aichat.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.modules.aichat.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aichatService;

    @GetMapping("/entry")
    public ApiResponse<String> entry() {
        return ApiResponse.success(aichatService.entry());
    }
}
