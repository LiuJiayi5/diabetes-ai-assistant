package com.diabetes.assistant.modules.risk.service;

import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.risk.dto.AdminRiskListItem;
import com.diabetes.assistant.modules.risk.dto.RiskDetailResponse;
import com.diabetes.assistant.modules.risk.dto.RiskEntryResponse;
import com.diabetes.assistant.modules.risk.dto.RiskHistoryItem;
import com.diabetes.assistant.modules.risk.dto.RiskPredictResponse;
import com.diabetes.assistant.modules.risk.dto.RiskTrendResponse;
import com.diabetes.assistant.modules.risk.dto.SimilarCaseItem;

import java.time.LocalDate;
import java.util.List;

public interface RiskService {

    RiskEntryResponse getEntry(Integer userId);

    RiskPredictResponse predictRisk(Integer userId);

    RiskDetailResponse getLatestAssessment(Integer userId);

    PageResult<RiskHistoryItem> getHistory(Integer userId, Integer page, Integer pageSize,
                                           LocalDate startDate, LocalDate endDate);

    RiskDetailResponse getAssessmentDetail(Integer userId, Integer assessmentId);

    PageResult<AdminRiskListItem> adminListAssessments(Integer userId, String riskLevel,
                                                       LocalDate startDate, LocalDate endDate,
                                                       Integer page, Integer pageSize);

    RiskDetailResponse adminGetAssessmentDetail(Integer assessmentId);

    RiskTrendResponse getRiskTrend(Integer userId);

    RiskTrendResponse adminGetRiskTrend(Integer userId);

    List<SimilarCaseItem> adminGetSimilarCases(Integer assessmentId, Integer limit);
}
