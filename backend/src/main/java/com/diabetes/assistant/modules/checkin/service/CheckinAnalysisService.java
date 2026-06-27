package com.diabetes.assistant.modules.checkin.service;

import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.checkin.dto.CheckinAnalysisResponse;

import java.time.LocalDate;

public interface CheckinAnalysisService {

    CheckinAnalysisResponse generateAnalysis(Integer userId, Integer period);

    CheckinAnalysisResponse getLatestAnalysis(Integer userId);

    PageResult<CheckinAnalysisResponse> listHistory(Integer userId, Integer page, Integer pageSize,
                                                    LocalDate startDate, LocalDate endDate);
}
