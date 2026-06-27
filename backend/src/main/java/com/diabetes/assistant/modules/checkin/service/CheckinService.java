package com.diabetes.assistant.modules.checkin.service;

import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.checkin.dto.CheckinHistoryQuery;
import com.diabetes.assistant.modules.checkin.dto.CheckinStatisticsResponse;
import com.diabetes.assistant.modules.checkin.dto.CheckinSubmitRequest;
import com.diabetes.assistant.modules.checkin.dto.CheckinTaskResponse;
import com.diabetes.assistant.modules.checkin.dto.CheckinTodayResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CheckinService {

    String entry();

    CheckinTodayResponse getTodayTasks(Integer userId, LocalDate date);

    CheckinTaskResponse submitCheckin(Integer userId, CheckinSubmitRequest request);

    PageResult<CheckinTaskResponse> listHistory(Integer userId, CheckinHistoryQuery query);

    CheckinStatisticsResponse getStatistics(Integer userId, Integer period);

    List<CheckinTaskResponse> listRecentCheckins(Integer userId, Integer period);

    BigDecimal getRecentCompletionRate(Integer userId, Integer period);

    String getLatestCheckinSummaryByUserId(Integer userId, Integer period);
}
