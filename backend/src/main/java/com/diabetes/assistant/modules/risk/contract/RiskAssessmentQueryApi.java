package com.diabetes.assistant.modules.risk.contract;

import com.diabetes.assistant.modules.risk.contract.dto.RiskAssessmentDTO;

import java.time.LocalDate;
import java.util.List;

public interface RiskAssessmentQueryApi {

    RiskAssessmentDTO getLatestAssessmentByUserId(Integer userId);

    List<RiskAssessmentDTO> listAssessmentsByUserId(Integer userId, LocalDate startDate, LocalDate endDate);

    String getLatestRiskSummaryByUserId(Integer userId);
}
