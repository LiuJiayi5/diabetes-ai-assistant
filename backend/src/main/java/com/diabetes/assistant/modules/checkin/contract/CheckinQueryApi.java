package com.diabetes.assistant.modules.checkin.contract;

import com.diabetes.assistant.modules.checkin.contract.dto.CheckinAnalysisDTO;
import com.diabetes.assistant.modules.checkin.contract.dto.CheckinRecordDTO;

import java.math.BigDecimal;
import java.util.List;

public interface CheckinQueryApi {

    List<CheckinRecordDTO> listRecentCheckins(Integer userId, Integer period);

    BigDecimal getRecentCompletionRate(Integer userId, Integer period);

    String getLatestCheckinSummaryByUserId(Integer userId, Integer period);

    CheckinAnalysisDTO getLatestAnalysisByUserId(Integer userId);

    String getLatestCheckinAnalysisSummaryByUserId(Integer userId);
}
