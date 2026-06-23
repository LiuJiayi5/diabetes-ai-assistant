package com.diabetes.assistant.modules.aichat.contract;

import com.diabetes.assistant.modules.aichat.contract.dto.AiChatSessionSummaryDTO;

import java.util.List;

public interface AiChatQueryApi {

    long countMessagesByUserId(Integer userId);

    List<AiChatSessionSummaryDTO> listRecentSessionsByUserId(Integer userId, Integer limit);

    String getLatestChatSummaryByUserId(Integer userId);
}
