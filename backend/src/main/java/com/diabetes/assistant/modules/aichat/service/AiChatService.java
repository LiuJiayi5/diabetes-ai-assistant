package com.diabetes.assistant.modules.aichat.service;

import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.aichat.dto.AiChatHistoryMessageResponse;
import com.diabetes.assistant.modules.aichat.dto.AiChatMessageRequest;
import com.diabetes.assistant.modules.aichat.dto.AiChatMessageResponse;
import com.diabetes.assistant.modules.aichat.dto.AiChatSessionResponse;
import com.diabetes.assistant.modules.aichat.dto.admin.AdminAiChatLogResponse;
import com.diabetes.assistant.modules.aichat.dto.admin.AdminAiChatQuery;
import com.diabetes.assistant.modules.aichat.dto.admin.AdminAiChatSessionResponse;

import java.util.List;

public interface AiChatService {

    String entry();

    AiChatMessageResponse sendMessage(Integer userId, AiChatMessageRequest request);

    PageResult<AiChatSessionResponse> listSessions(Integer userId, Integer page, Integer pageSize);

    List<AiChatHistoryMessageResponse> listMessages(Integer userId, Integer sessionId);

    void deleteSession(Integer userId, Integer sessionId);

    void clearSession(Integer userId, Integer sessionId);

    PageResult<AdminAiChatLogResponse> listAdminLogs(AdminAiChatQuery query);

    PageResult<AdminAiChatSessionResponse> listAdminSessions(AdminAiChatQuery query);

    AdminAiChatLogResponse getAdminMessageDetail(Integer messageId);
}
