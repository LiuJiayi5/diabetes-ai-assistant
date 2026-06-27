package com.diabetes.assistant.modules.aichat.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.aichat.dto.AiChatHistoryMessageResponse;
import com.diabetes.assistant.modules.aichat.dto.AiChatMessageRequest;
import com.diabetes.assistant.modules.aichat.dto.AiChatMessageResponse;
import com.diabetes.assistant.modules.aichat.dto.AiChatSessionResponse;
import com.diabetes.assistant.modules.aichat.service.AiChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai-chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aichatService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping("/entry")
    public ApiResponse<String> entry() {
        return ApiResponse.success(aichatService.entry());
    }

    @PostMapping("/message")
    public ApiResponse<AiChatMessageResponse> sendMessage(
            HttpServletRequest request,
            @Valid @RequestBody AiChatMessageRequest messageRequest) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(aichatService.sendMessage(userId, messageRequest));
    }

    @GetMapping("/sessions")
    public ApiResponse<PageResult<AiChatSessionResponse>> sessions(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(aichatService.listSessions(userId, page, pageSize));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<AiChatHistoryMessageResponse>> messages(
            HttpServletRequest request,
            @PathVariable Integer sessionId) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(aichatService.listMessages(userId, sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(
            HttpServletRequest request,
            @PathVariable Integer sessionId) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        aichatService.deleteSession(userId, sessionId);
        return ApiResponse.success();
    }

    @PostMapping("/sessions/{sessionId}/clear")
    public ApiResponse<Void> clearSession(
            HttpServletRequest request,
            @PathVariable Integer sessionId) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        aichatService.clearSession(userId, sessionId);
        return ApiResponse.success();
    }
}
