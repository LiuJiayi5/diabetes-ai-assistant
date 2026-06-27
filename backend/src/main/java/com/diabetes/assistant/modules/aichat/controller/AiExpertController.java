package com.diabetes.assistant.modules.aichat.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.modules.aichat.dto.AiExpertResponse;
import com.diabetes.assistant.modules.aichat.service.AiExpertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai-chat/experts")
@RequiredArgsConstructor
public class AiExpertController {

    private final AiExpertService aiExpertService;

    @GetMapping
    public ApiResponse<List<AiExpertResponse>> experts() {
        return ApiResponse.success(aiExpertService.listEnabledExperts());
    }
}
