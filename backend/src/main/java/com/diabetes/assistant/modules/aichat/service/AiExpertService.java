package com.diabetes.assistant.modules.aichat.service;

import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.aichat.dto.AiExpertResponse;
import com.diabetes.assistant.modules.aichat.dto.AiExpertSaveRequest;
import com.diabetes.assistant.modules.aichat.entity.AiExpert;

import java.util.List;

public interface AiExpertService {

    List<AiExpertResponse> listEnabledExperts();

    PageResult<AiExpertResponse> listAdminExperts(Integer page, Integer pageSize, String keyword, String status);

    AiExpertResponse getAdminExpert(Integer expertId);

    AiExpertResponse saveAdminExpert(Integer expertId, AiExpertSaveRequest request);

    void deleteAdminExpert(Integer expertId);

    AiExpert requireEnabledExpert(Integer expertId);

    AiExpert getDefaultEnabledExpert();
}
