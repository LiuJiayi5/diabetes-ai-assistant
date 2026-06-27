package com.diabetes.assistant.modules.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.assistant.modules.aichat.contract.AiChatQueryApi;
import com.diabetes.assistant.modules.aichat.contract.dto.AiChatSessionSummaryDTO;
import com.diabetes.assistant.modules.aichat.entity.AiChatMessage;
import com.diabetes.assistant.modules.aichat.entity.AiChatSession;
import com.diabetes.assistant.modules.aichat.mapper.AiChatMessageMapper;
import com.diabetes.assistant.modules.aichat.mapper.AiChatSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiChatQueryApiImpl implements AiChatQueryApi {

    private static final String SESSION_STATUS_ACTIVE = "active";

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;

    @Override
    public long countMessagesByUserId(Integer userId) {
        Long count = messageMapper.selectCount(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getUserId, userId));
        return count == null ? 0 : count;
    }

    @Override
    public List<AiChatSessionSummaryDTO> listRecentSessionsByUserId(Integer userId, Integer limit) {
        int normalizedLimit = limit == null || limit < 1 ? 5 : Math.min(limit, 20);
        return sessionMapper.selectList(new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .eq(AiChatSession::getStatus, SESSION_STATUS_ACTIVE)
                        .orderByDesc(AiChatSession::getLastMessageTime)
                        .orderByDesc(AiChatSession::getCreateTime)
                        .last("LIMIT " + normalizedLimit))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public String getLatestChatSummaryByUserId(Integer userId) {
        AiChatMessage latest = messageMapper.selectOne(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getUserId, userId)
                .orderByDesc(AiChatMessage::getCreateTime)
                .orderByDesc(AiChatMessage::getMessageId)
                .last("LIMIT 1"));
        if (latest == null) {
            return "No AI doctor consultation history";
        }
        String question = StringUtils.hasText(latest.getUserMessage()) ? latest.getUserMessage() : "";
        String answer = StringUtils.hasText(latest.getAiResponse()) ? latest.getAiResponse() : "";
        return "Latest AI doctor consultation: user asked [" + abbreviate(question, 120)
                + "], AI replied [" + abbreviate(answer, 180) + "]";
    }

    private AiChatSessionSummaryDTO toSummary(AiChatSession session) {
        AiChatSessionSummaryDTO dto = new AiChatSessionSummaryDTO();
        dto.setSessionId(session.getSessionId());
        dto.setUserId(session.getUserId());
        dto.setSessionTitle(session.getSessionTitle());
        dto.setStatus(session.getStatus());
        dto.setLastMessageTime(session.getLastMessageTime());
        dto.setCreateTime(session.getCreateTime());
        return dto;
    }

    private String abbreviate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
