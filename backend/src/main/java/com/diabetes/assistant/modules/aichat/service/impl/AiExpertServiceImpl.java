package com.diabetes.assistant.modules.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.aichat.dto.AiExpertResponse;
import com.diabetes.assistant.modules.aichat.dto.AiExpertSaveRequest;
import com.diabetes.assistant.modules.aichat.entity.AiChatSession;
import com.diabetes.assistant.modules.aichat.entity.AiExpert;
import com.diabetes.assistant.modules.aichat.mapper.AiChatSessionMapper;
import com.diabetes.assistant.modules.aichat.mapper.AiExpertMapper;
import com.diabetes.assistant.modules.aichat.service.AiExpertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiExpertServiceImpl implements AiExpertService {

    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DISABLED = "disabled";

    private final AiExpertMapper expertMapper;
    private final AiChatSessionMapper sessionMapper;

    @Override
    public List<AiExpertResponse> listEnabledExperts() {
        return expertMapper.selectList(baseWrapper()
                        .eq(AiExpert::getStatus, STATUS_ENABLED))
                .stream()
                .map(expert -> toResponse(expert, false))
                .toList();
    }

    @Override
    public PageResult<AiExpertResponse> listAdminExperts(Integer page, Integer pageSize, String keyword, String status) {
        int currentPage = normalizePage(page);
        int size = normalizePageSize(pageSize);
        String normalizedKeyword = normalize(keyword);
        String normalizedStatus = normalize(status);
        LambdaQueryWrapper<AiExpert> wrapper = baseWrapper()
                .eq(StringUtils.hasText(normalizedStatus), AiExpert::getStatus, normalizedStatus)
                .and(StringUtils.hasText(normalizedKeyword), w -> w
                        .like(AiExpert::getExpertName, normalizedKeyword)
                        .or()
                        .like(AiExpert::getTitle, normalizedKeyword)
                        .or()
                        .like(AiExpert::getDepartment, normalizedKeyword)
                        .or()
                        .like(AiExpert::getSpecialty, normalizedKeyword));
        Page<AiExpert> result = expertMapper.selectPage(Page.of(currentPage, size), wrapper);
        List<AiExpertResponse> list = result.getRecords().stream()
                .map(expert -> toResponse(expert, true))
                .toList();
        return new PageResult<>(list, result.getTotal(), currentPage, size);
    }

    @Override
    public AiExpertResponse getAdminExpert(Integer expertId) {
        return toResponse(requireExistingExpert(expertId), true);
    }

    @Override
    @Transactional
    public AiExpertResponse saveAdminExpert(Integer expertId, AiExpertSaveRequest request) {
        AiExpert expert = expertId == null ? new AiExpert() : requireExistingExpert(expertId);
        AiExpertSaveRequest safeRequest = request == null ? new AiExpertSaveRequest() : request;
        expert.setExpertName(required(safeRequest.getExpertName(), "专家姓名不能为空"));
        expert.setTitle(normalize(safeRequest.getTitle()));
        expert.setDepartment(normalize(safeRequest.getDepartment()));
        expert.setAvatarUrl(normalize(safeRequest.getAvatarUrl()));
        expert.setSpecialty(normalize(safeRequest.getSpecialty()));
        expert.setPersona(normalize(safeRequest.getPersona()));
        expert.setOpeningMessage(normalize(safeRequest.getOpeningMessage()));
        expert.setSortOrder(safeRequest.getSortOrder() == null ? 0 : safeRequest.getSortOrder());
        expert.setStatus(normalizeStatus(safeRequest.getStatus()));
        expert.setUpdateTime(LocalDateTime.now());
        if (expert.getExpertId() == null) {
            expert.setCreateTime(LocalDateTime.now());
            expertMapper.insert(expert);
        } else {
            expertMapper.updateById(expert);
        }
        return toResponse(expert, true);
    }

    @Override
    @Transactional
    public void deleteAdminExpert(Integer expertId) {
        AiExpert expert = requireExistingExpert(expertId);
        Long sessions = sessionMapper.selectCount(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getExpertId, expertId));
        if (sessions != null && sessions > 0) {
            expert.setStatus(STATUS_DISABLED);
            expert.setUpdateTime(LocalDateTime.now());
            expertMapper.updateById(expert);
            return;
        }
        expertMapper.deleteById(expertId);
    }

    @Override
    public AiExpert requireEnabledExpert(Integer expertId) {
        if (expertId == null) {
            AiExpert fallback = getDefaultEnabledExpert();
            if (fallback == null) {
                throw new BusinessException(404, "暂无可用AI专家");
            }
            return fallback;
        }
        AiExpert expert = expertMapper.selectById(expertId);
        if (expert == null || !STATUS_ENABLED.equals(expert.getStatus())) {
            throw new BusinessException(404, "AI专家不存在或已停用");
        }
        return expert;
    }

    @Override
    public AiExpert getDefaultEnabledExpert() {
        return expertMapper.selectOne(baseWrapper()
                .eq(AiExpert::getStatus, STATUS_ENABLED)
                .last("LIMIT 1"));
    }

    private AiExpert requireExistingExpert(Integer expertId) {
        if (expertId == null) {
            throw new BusinessException(400, "expert_id不能为空");
        }
        AiExpert expert = expertMapper.selectById(expertId);
        if (expert == null) {
            throw new BusinessException(404, "AI专家不存在");
        }
        return expert;
    }

    private AiExpertResponse toResponse(AiExpert expert, boolean includeSessionCount) {
        AiExpertResponse response = new AiExpertResponse();
        response.setExpertId(expert.getExpertId());
        response.setExpertName(expert.getExpertName());
        response.setTitle(expert.getTitle());
        response.setDepartment(expert.getDepartment());
        response.setAvatarUrl(expert.getAvatarUrl());
        response.setSpecialty(expert.getSpecialty());
        response.setPersona(expert.getPersona());
        response.setOpeningMessage(expert.getOpeningMessage());
        response.setSortOrder(expert.getSortOrder());
        response.setStatus(expert.getStatus());
        response.setCreateTime(expert.getCreateTime());
        response.setUpdateTime(expert.getUpdateTime());
        if (includeSessionCount) {
            response.setSessionCount(sessionMapper.selectCount(new LambdaQueryWrapper<AiChatSession>()
                    .eq(AiChatSession::getExpertId, expert.getExpertId())));
        }
        return response;
    }

    private LambdaQueryWrapper<AiExpert> baseWrapper() {
        return new LambdaQueryWrapper<AiExpert>()
                .orderByAsc(AiExpert::getSortOrder)
                .orderByAsc(AiExpert::getExpertId);
    }

    private String normalizeStatus(String status) {
        String normalized = normalize(status);
        if (!StringUtils.hasText(normalized)) {
            return STATUS_ENABLED;
        }
        if (!STATUS_ENABLED.equals(normalized) && !STATUS_DISABLED.equals(normalized)) {
            throw new BusinessException(400, "status只能是enabled或disabled");
        }
        return normalized;
    }

    private String required(String value, String message) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(400, message);
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
