package com.diabetes.assistant.modules.risk.service.impl;

import com.diabetes.assistant.modules.risk.contract.RiskAssessmentQueryApi;
import com.diabetes.assistant.modules.risk.contract.dto.RiskAssessmentDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class RiskAssessmentQueryApiImpl implements RiskAssessmentQueryApi {

    @Override
    public RiskAssessmentDTO getLatestAssessmentByUserId(Integer userId) {
        return null;
    }

    @Override
    public List<RiskAssessmentDTO> listAssessmentsByUserId(Integer userId, LocalDate startDate, LocalDate endDate) {
        return List.of();
    }

    @Override
    public String getLatestRiskSummaryByUserId(Integer userId) {
        return "No risk assessment data";
    }
}
