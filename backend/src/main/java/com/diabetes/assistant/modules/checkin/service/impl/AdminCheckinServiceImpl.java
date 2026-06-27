package com.diabetes.assistant.modules.checkin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminAnalysisQuery;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminApiCallLogResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminCheckinAnalysisResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminCheckinOverviewResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminCheckinQuery;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminCheckinRecordResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminInactiveUserResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.AdminLogQuery;
import com.diabetes.assistant.modules.checkin.dto.admin.PatientSummaryResponse;
import com.diabetes.assistant.modules.checkin.dto.admin.PlanSummaryResponse;
import com.diabetes.assistant.modules.checkin.entity.ApiCallLog;
import com.diabetes.assistant.modules.checkin.entity.CheckinAnalysis;
import com.diabetes.assistant.modules.checkin.entity.CheckinRecord;
import com.diabetes.assistant.modules.checkin.mapper.ApiCallLogMapper;
import com.diabetes.assistant.modules.checkin.mapper.CheckinAnalysisMapper;
import com.diabetes.assistant.modules.checkin.mapper.CheckinRecordMapper;
import com.diabetes.assistant.modules.checkin.service.AdminCheckinService;
import com.diabetes.assistant.modules.lifeplan.contract.LifePlanQueryApi;
import com.diabetes.assistant.modules.lifeplan.contract.dto.LifePlanDTO;
import com.diabetes.assistant.modules.profile.contract.PatientProfileQueryApi;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import com.diabetes.assistant.modules.user.contract.dto.UserBasicDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCheckinServiceImpl implements AdminCheckinService {

    private static final String STATUS_COMPLETED = "completed";
    private static final String TASK_TYPE_DIET = "diet";
    private static final String TASK_TYPE_EXERCISE = "exercise";
    private static final String SERVICE_CHECKIN_ANALYSIS = "checkin_analysis";

    private final CheckinRecordMapper checkinRecordMapper;
    private final CheckinAnalysisMapper checkinAnalysisMapper;
    private final ApiCallLogMapper apiCallLogMapper;
    private final UserQueryApi userQueryApi;
    private final LifePlanQueryApi lifePlanQueryApi;
    private final PatientProfileQueryApi patientProfileQueryApi;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<AdminCheckinRecordResponse> listRecords(AdminCheckinQuery query) {
        int page = normalizePage(query.getPage());
        int pageSize = normalizePageSize(query.getPageSize());
        LambdaQueryWrapper<CheckinRecord> wrapper = buildRecordWrapper(query)
                .orderByDesc(CheckinRecord::getCheckinDate)
                .orderByDesc(CheckinRecord::getCreateTime);
        Page<CheckinRecord> result = checkinRecordMapper.selectPage(Page.of(page, pageSize), wrapper);
        List<AdminCheckinRecordResponse> list = result.getRecords().stream()
                .map(this::toRecordResponse)
                .toList();
        return new PageResult<>(list, result.getTotal(), page, pageSize);
    }

    @Override
    public AdminCheckinRecordResponse getRecordDetail(Integer checkinId) {
        CheckinRecord record = checkinRecordMapper.selectById(checkinId);
        if (record == null) {
            throw new BusinessException(404, "打卡记录不存在");
        }
        return toRecordResponse(record);
    }

    @Override
    public AdminCheckinOverviewResponse getOverview(LocalDate startDate, LocalDate endDate, Integer userId) {
        LocalDate normalizedEnd = endDate == null ? LocalDate.now() : endDate;
        LocalDate normalizedStart = startDate == null ? normalizedEnd.minusDays(6) : startDate;
        if (normalizedStart.isAfter(normalizedEnd)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }

        List<CheckinRecord> records = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(userId != null, CheckinRecord::getUserId, userId)
                .ge(CheckinRecord::getCheckinDate, normalizedStart)
                .le(CheckinRecord::getCheckinDate, normalizedEnd));

        int total = records.size();
        int completed = (int) records.stream().filter(this::isCompleted).count();
        int dietTotal = (int) records.stream().filter(record -> TASK_TYPE_DIET.equals(record.getTaskType())).count();
        int dietCompleted = (int) records.stream()
                .filter(record -> TASK_TYPE_DIET.equals(record.getTaskType()))
                .filter(this::isCompleted)
                .count();
        int exerciseTotal = (int) records.stream().filter(record -> TASK_TYPE_EXERCISE.equals(record.getTaskType())).count();
        int exerciseCompleted = (int) records.stream()
                .filter(record -> TASK_TYPE_EXERCISE.equals(record.getTaskType()))
                .filter(this::isCompleted)
                .count();

        Map<LocalDate, List<CheckinRecord>> byDate = records.stream()
                .collect(Collectors.groupingBy(CheckinRecord::getCheckinDate, LinkedHashMap::new, Collectors.toList()));
        List<AdminCheckinOverviewResponse.DailyStat> dailyStats = new ArrayList<>();
        for (LocalDate date = normalizedStart; !date.isAfter(normalizedEnd); date = date.plusDays(1)) {
            List<CheckinRecord> dayRecords = byDate.getOrDefault(date, List.of());
            int dayTotal = dayRecords.size();
            int dayCompleted = (int) dayRecords.stream().filter(this::isCompleted).count();
            AdminCheckinOverviewResponse.DailyStat stat = new AdminCheckinOverviewResponse.DailyStat();
            stat.setDate(date);
            stat.setTotalCount(dayTotal);
            stat.setCompletedCount(dayCompleted);
            stat.setCompletionRate(calculateRate(dayCompleted, dayTotal));
            dailyStats.add(stat);
        }

        AdminCheckinOverviewResponse response = new AdminCheckinOverviewResponse();
        response.setStartDate(normalizedStart);
        response.setEndDate(normalizedEnd);
        response.setTotalTaskCount(total);
        response.setCompletedTaskCount(completed);
        response.setUnfinishedTaskCount(total - completed);
        response.setDietTotalCount(dietTotal);
        response.setDietCompletedCount(dietCompleted);
        response.setExerciseTotalCount(exerciseTotal);
        response.setExerciseCompletedCount(exerciseCompleted);
        response.setCompletionRate(calculateRate(completed, total));
        response.setDailyStats(dailyStats);
        return response;
    }

    @Override
    public PageResult<AdminCheckinAnalysisResponse> listAnalyses(AdminAnalysisQuery query) {
        int page = normalizePage(query.getPage());
        int pageSize = normalizePageSize(query.getPageSize());
        LambdaQueryWrapper<CheckinAnalysis> wrapper = buildAnalysisWrapper(query)
                .orderByDesc(CheckinAnalysis::getCreateTime);
        Page<CheckinAnalysis> result = checkinAnalysisMapper.selectPage(Page.of(page, pageSize), wrapper);
        List<AdminCheckinAnalysisResponse> list = result.getRecords().stream()
                .map(this::toAnalysisResponse)
                .toList();
        return new PageResult<>(list, result.getTotal(), page, pageSize);
    }

    @Override
    public AdminCheckinAnalysisResponse getAnalysisDetail(Integer analysisId) {
        CheckinAnalysis analysis = checkinAnalysisMapper.selectById(analysisId);
        if (analysis == null) {
            throw new BusinessException(404, "分析记录不存在");
        }
        return toAnalysisResponse(analysis);
    }

    @Override
    public List<AdminInactiveUserResponse> listInactiveUsers(Integer days, Integer limit) {
        int normalizedDays = days == null || days < 1 ? 7 : Math.min(days, 90);
        int normalizedLimit = limit == null || limit < 1 ? 20 : Math.min(limit, 100);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(normalizedDays - 1L);

        List<CheckinRecord> allRecords = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .orderByDesc(CheckinRecord::getCheckinDate));
        Map<Integer, List<CheckinRecord>> byUser = allRecords.stream()
                .collect(Collectors.groupingBy(CheckinRecord::getUserId));

        List<AdminInactiveUserResponse> responses = new ArrayList<>();
        for (Map.Entry<Integer, List<CheckinRecord>> entry : byUser.entrySet()) {
            Integer userId = entry.getKey();
            UserBasicDTO user = userQueryApi.getUserBasicById(userId);
            if (user == null || !"patient".equals(user.getRole()) || !"active".equals(user.getStatus())) {
                continue;
            }

            List<CheckinRecord> records = entry.getValue();
            LocalDate lastCompletedDate = records.stream()
                    .filter(this::isCompleted)
                    .map(CheckinRecord::getCheckinDate)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            LocalDate lastAnyDate = records.stream()
                    .map(CheckinRecord::getCheckinDate)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            List<CheckinRecord> recentRecords = records.stream()
                    .filter(record -> !record.getCheckinDate().isBefore(startDate))
                    .filter(record -> !record.getCheckinDate().isAfter(endDate))
                    .toList();
            int recentTotal = recentRecords.size();
            int recentCompleted = (int) recentRecords.stream().filter(this::isCompleted).count();
            BigDecimal recentRate = calculateRate(recentCompleted, recentTotal);
            int inactiveDays = lastCompletedDate == null
                    ? normalizedDays
                    : (int) ChronoUnit.DAYS.between(lastCompletedDate, endDate);

            boolean longNoCompleted = lastCompletedDate == null || inactiveDays >= normalizedDays;
            boolean lowCompletion = recentTotal > 0 && recentRate.compareTo(BigDecimal.valueOf(30)) < 0;
            boolean noRecentRecords = recentTotal == 0 && (lastAnyDate == null || ChronoUnit.DAYS.between(lastAnyDate, endDate) >= normalizedDays);
            if (!longNoCompleted && !lowCompletion && !noRecentRecords) {
                continue;
            }

            AdminInactiveUserResponse response = new AdminInactiveUserResponse();
            response.setPatient(toPatientSummary(userId));
            response.setLastCheckinDate(lastCompletedDate);
            response.setRecentTotalCount(recentTotal);
            response.setRecentCompletedCount(recentCompleted);
            response.setRecentCompletionRate(recentRate);
            response.setInactiveDays(inactiveDays);
            response.setReason(buildInactiveReason(longNoCompleted, noRecentRecords, lowCompletion, normalizedDays));
            responses.add(response);
        }

        return responses.stream()
                .sorted(Comparator.comparing(AdminInactiveUserResponse::getRecentCompletionRate)
                        .thenComparing(AdminInactiveUserResponse::getInactiveDays, Comparator.reverseOrder()))
                .limit(normalizedLimit)
                .toList();
    }

    @Override
    public PageResult<AdminApiCallLogResponse> listAnalysisLogs(AdminLogQuery query) {
        int page = normalizePage(query.getPage());
        int pageSize = normalizePageSize(query.getPageSize());
        LocalDate startDate = query.getStartDate();
        LocalDate endDate = query.getEndDate();
        LambdaQueryWrapper<ApiCallLog> wrapper = new LambdaQueryWrapper<ApiCallLog>()
                .eq(ApiCallLog::getServiceType, SERVICE_CHECKIN_ANALYSIS)
                .eq(query.getUserId() != null, ApiCallLog::getUserId, query.getUserId())
                .eq(StringUtils.hasText(query.getCallStatus()), ApiCallLog::getCallStatus, query.getCallStatus())
                .ge(startDate != null, ApiCallLog::getCreateTime, startDate == null ? null : startDate.atStartOfDay())
                .lt(endDate != null, ApiCallLog::getCreateTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByDesc(ApiCallLog::getCreateTime);
        Page<ApiCallLog> result = apiCallLogMapper.selectPage(Page.of(page, pageSize), wrapper);
        List<AdminApiCallLogResponse> list = result.getRecords().stream()
                .map(this::toLogResponse)
                .toList();
        return new PageResult<>(list, result.getTotal(), page, pageSize);
    }

    private LambdaQueryWrapper<CheckinRecord> buildRecordWrapper(AdminCheckinQuery query) {
        Integer keywordUserId = parseUserId(query.getPatientKeyword());
        return new LambdaQueryWrapper<CheckinRecord>()
                .eq(query.getUserId() != null, CheckinRecord::getUserId, query.getUserId())
                .eq(query.getUserId() == null && keywordUserId != null, CheckinRecord::getUserId, keywordUserId)
                .ge(query.getStartDate() != null, CheckinRecord::getCheckinDate, query.getStartDate())
                .le(query.getEndDate() != null, CheckinRecord::getCheckinDate, query.getEndDate())
                .eq(StringUtils.hasText(query.getTaskType()), CheckinRecord::getTaskType, query.getTaskType())
                .eq(StringUtils.hasText(query.getStatus()), CheckinRecord::getStatus, query.getStatus());
    }

    private LambdaQueryWrapper<CheckinAnalysis> buildAnalysisWrapper(AdminAnalysisQuery query) {
        Integer keywordUserId = parseUserId(query.getPatientKeyword());
        return new LambdaQueryWrapper<CheckinAnalysis>()
                .eq(query.getUserId() != null, CheckinAnalysis::getUserId, query.getUserId())
                .eq(query.getUserId() == null && keywordUserId != null, CheckinAnalysis::getUserId, keywordUserId)
                .ge(query.getStartDate() != null, CheckinAnalysis::getStartDate, query.getStartDate())
                .le(query.getEndDate() != null, CheckinAnalysis::getEndDate, query.getEndDate())
                .eq(StringUtils.hasText(query.getCallStatus()), CheckinAnalysis::getCallStatus, query.getCallStatus());
    }

    private AdminCheckinRecordResponse toRecordResponse(CheckinRecord record) {
        AdminCheckinRecordResponse response = new AdminCheckinRecordResponse();
        response.setCheckinId(record.getCheckinId());
        response.setUserId(record.getUserId());
        response.setPlanId(record.getPlanId());
        response.setTaskType(record.getTaskType());
        response.setTaskName(record.getTaskName());
        response.setStatus(record.getStatus());
        response.setNote(record.getNote());
        response.setCheckinDate(record.getCheckinDate());
        response.setCompletedTime(record.getCompletedTime());
        response.setCreateTime(record.getCreateTime());
        response.setPatient(toPatientSummary(record.getUserId()));
        response.setPlan(toPlanSummary(record.getUserId(), record.getPlanId()));
        return response;
    }

    private AdminCheckinAnalysisResponse toAnalysisResponse(CheckinAnalysis analysis) {
        AdminCheckinAnalysisResponse response = new AdminCheckinAnalysisResponse();
        response.setAnalysisId(analysis.getAnalysisId());
        response.setUserId(analysis.getUserId());
        response.setPlanId(analysis.getPlanId());
        response.setStartDate(analysis.getStartDate());
        response.setEndDate(analysis.getEndDate());
        response.setTotalDays(analysis.getTotalDays());
        response.setDietCompletionCount(analysis.getDietCompletionCount());
        response.setExerciseCompletionCount(analysis.getExerciseCompletionCount());
        response.setCompletionRate(analysis.getCompletionRate());
        response.setHabitScore(analysis.getHabitScore());
        response.setDietSummary(analysis.getDietSummary());
        response.setExerciseSummary(analysis.getExerciseSummary());
        response.setLifeEvaluation(analysis.getLifeEvaluation());
        response.setMainProblems(parseStringList(analysis.getMainProblems()));
        response.setImprovementSuggestions(parseStringList(analysis.getImprovementSuggestions()));
        response.setNextFocus(analysis.getNextFocus());
        response.setSummary(analysis.getSummary());
        response.setInputSummary(analysis.getInputSummary());
        response.setInputItems(parseInputItems(analysis.getInputSummary()));
        response.setCallStatus(analysis.getCallStatus());
        response.setErrorMessage(analysis.getErrorMessage());
        response.setCreateTime(analysis.getCreateTime());
        response.setPatient(toPatientSummary(analysis.getUserId()));
        response.setPlan(toPlanSummary(analysis.getUserId(), analysis.getPlanId()));
        return response;
    }

    private AdminApiCallLogResponse toLogResponse(ApiCallLog log) {
        AdminApiCallLogResponse response = new AdminApiCallLogResponse();
        response.setLogId(log.getLogId());
        response.setUserId(log.getUserId());
        response.setServiceType(log.getServiceType());
        response.setRequestSummary(log.getRequestSummary());
        response.setResponseSummary(log.getResponseSummary());
        response.setCallStatus(log.getCallStatus());
        response.setErrorMessage(log.getErrorMessage());
        response.setCreateTime(log.getCreateTime());
        response.setPatient(toPatientSummary(log.getUserId()));
        return response;
    }

    private PatientSummaryResponse toPatientSummary(Integer userId) {
        if (userId == null) {
            return null;
        }
        UserBasicDTO user = userQueryApi.getUserBasicById(userId);
        PatientSummaryResponse response = new PatientSummaryResponse();
        response.setUserId(userId);
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setPhone(maskPhone(user.getPhone()));
            response.setEmail(user.getEmail());
            response.setStatus(user.getStatus());
        } else {
            response.setUsername("未知用户");
        }
        response.setProfileSummary(safeProfileSummary(userId));
        return response;
    }

    private PlanSummaryResponse toPlanSummary(Integer userId, Integer planId) {
        PlanSummaryResponse response = new PlanSummaryResponse();
        response.setPlanId(planId);
        try {
            LifePlanDTO plan = lifePlanQueryApi.getCurrentPlanByUserId(userId);
            if (plan != null && Objects.equals(plan.getPlanId(), planId)) {
                response.setPlanTitle(plan.getPlanTitle());
                response.setPlanGoal(plan.getPlanGoal());
                response.setSummary(plan.getSummary());
                response.setStatus(plan.getStatus());
                return response;
            }
        } catch (Exception ignored) {
            // The admin page can still show check-in data when another module is unavailable.
        }
        response.setPlanTitle(planId == null ? "无关联方案" : "历史或非当前方案");
        response.setSummary(planId == null ? "该打卡未关联生活方案。" : "当前生活方案 contract 未返回该历史方案详情。");
        return response;
    }

    private List<String> parseStringList(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(value);
            if (node.isArray()) {
                List<String> list = new ArrayList<>();
                node.forEach(item -> list.add(item.asText()));
                return list;
            }
            if (node.isTextual()) {
                return List.of(node.asText());
            }
        } catch (Exception ignored) {
            return List.of(value);
        }
        return List.of();
    }

    private List<String> parseInputItems(String inputSummary) {
        if (!StringUtils.hasText(inputSummary)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(inputSummary);
            if (!root.isObject()) {
                return List.of(inputSummary);
            }
            List<String> items = new ArrayList<>();
            root.fields().forEachRemaining(entry -> items.add(entry.getKey() + ": " + entry.getValue().asText(entry.getValue().toString())));
            return items;
        } catch (JsonProcessingException exception) {
            return List.of(inputSummary);
        }
    }

    private String safeProfileSummary(Integer userId) {
        try {
            return patientProfileQueryApi.getProfileSummaryByUserId(userId);
        } catch (Exception exception) {
            return "暂无档案摘要";
        }
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private Integer parseUserId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isCompleted(CheckinRecord record) {
        return STATUS_COMPLETED.equals(record.getStatus());
    }

    private BigDecimal calculateRate(int completed, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private String buildInactiveReason(boolean longNoCompleted, boolean noRecentRecords, boolean lowCompletion, int days) {
        if (noRecentRecords) {
            return "\u8fd1 " + days + " \u5929\u6ca1\u6709\u6253\u5361\u8bb0\u5f55";
        }
        if (longNoCompleted) {
            return "\u8fd1 " + days + " \u5929\u6ca1\u6709\u5b8c\u6210\u6253\u5361";
        }
        if (lowCompletion) {
            return "\u8fd1\u671f\u5b8c\u6210\u7387\u4f4e\u4e8e 30%";
        }
        return "\u8fd1\u671f\u6253\u5361\u4e0d\u8db3";
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
