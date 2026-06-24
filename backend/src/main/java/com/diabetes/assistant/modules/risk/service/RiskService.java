package com.diabetes.assistant.modules.risk.service;

import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.risk.dto.AdminRiskListItem;
import com.diabetes.assistant.modules.risk.dto.RiskDetailResponse;
import com.diabetes.assistant.modules.risk.dto.RiskEntryResponse;
import com.diabetes.assistant.modules.risk.dto.RiskHistoryItem;
import com.diabetes.assistant.modules.risk.dto.RiskPredictResponse;

import java.time.LocalDate;

public interface RiskService {

    RiskEntryResponse getEntry();

    RiskPredictResponse predictRisk();

    RiskDetailResponse getLatestAssessment();

    PageResult<RiskHistoryItem> getHistory(Integer page, Integer pageSize,
                                           LocalDate startDate, LocalDate endDate);

    RiskDetailResponse getAssessmentDetail(Integer assessmentId);

    PageResult<AdminRiskListItem> adminListAssessments(Integer userId, String riskLevel,
                                                       LocalDate startDate, LocalDate endDate,
                                                       Integer page, Integer pageSize);

    RiskDetailResponse adminGetAssessmentDetail(Integer assessmentId);
}
