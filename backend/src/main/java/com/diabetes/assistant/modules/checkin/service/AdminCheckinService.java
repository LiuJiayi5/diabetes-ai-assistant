package com.diabetes.assistant.modules.checkin.service;

import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminAnalysisQuery;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminApiCallLogResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminCheckinAnalysisResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminCheckinOverviewResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminCheckinQuery;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminCheckinRecordResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminInactiveUserResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminLogQuery;

import java.time.LocalDate;
import java.util.List;

public interface AdminCheckinService {

    PageResult<AdminCheckinRecordResponse> listRecords(AdminCheckinQuery query);

    AdminCheckinRecordResponse getRecordDetail(Integer checkinId);

    AdminCheckinOverviewResponse getOverview(LocalDate startDate, LocalDate endDate, Integer userId);

    PageResult<AdminCheckinAnalysisResponse> listAnalyses(AdminAnalysisQuery query);

    AdminCheckinAnalysisResponse getAnalysisDetail(Integer analysisId);

    List<AdminInactiveUserResponse> listInactiveUsers(Integer days, Integer limit);

    PageResult<AdminApiCallLogResponse> listAnalysisLogs(AdminLogQuery query);

    AdminApiCallLogResponse getAnalysisLogDetail(Integer logId);
}
