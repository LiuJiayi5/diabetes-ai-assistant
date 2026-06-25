package com.diabetes.assistant.modules.checkin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.checkin.dto.CheckinAnalysisResponse;
import com.diabetes.assistant.modules.checkin.entity.ApiCallLog;
import com.diabetes.assistant.modules.checkin.entity.CheckinAnalysis;
import com.diabetes.assistant.modules.checkin.entity.CheckinRecord;
import com.diabetes.assistant.modules.checkin.mapper.ApiCallLogMapper;
import com.diabetes.assistant.modules.checkin.mapper.CheckinAnalysisMapper;
import com.diabetes.assistant.modules.checkin.mapper.CheckinRecordMapper;
import com.diabetes.assistant.modules.checkin.service.CheckinAnalysisService;
import com.diabetes.assistant.modules.dify.service.DifyService;
import com.diabetes.assistant.modules.healthmetric.contract.HealthMetricQueryApi;
import com.diabetes.assistant.modules.lifeplan.contract.LifePlanQueryApi;
import com.diabetes.assistant.modules.lifeplan.contract.dto.LifePlanDTO;
import com.diabetes.assistant.modules.profile.contract.PatientProfileQueryApi;
import com.diabetes.assistant.modules.risk.contract.RiskAssessmentQueryApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CheckinAnalysisServiceImpl implements CheckinAnalysisService {

    private static final String TASK_TYPE_DIET = "diet";
    private static final String TASK_TYPE_EXERCISE = "exercise";
    private static final String STATUS_COMPLETED = "completed";
    private static final String CALL_STATUS_SUCCESS = "success";
    private static final String CALL_STATUS_FAILED = "failed";
    private static final String SERVICE_CHECKIN_ANALYSIS = "checkin_analysis";

    private final CheckinRecordMapper checkinRecordMapper;
    private final CheckinAnalysisMapper checkinAnalysisMapper;
    private final ApiCallLogMapper apiCallLogMapper;
    private final LifePlanQueryApi lifePlanQueryApi;
    private final PatientProfileQueryApi patientProfileQueryApi;
    private final HealthMetricQueryApi healthMetricQueryApi;
    private final RiskAssessmentQueryApi riskAssessmentQueryApi;
    private final DifyService difyService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CheckinAnalysisResponse generateAnalysis(Integer userId, Integer period) {
        int normalizedPeriod = normalizePeriod(period);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(normalizedPeriod - 1L);
        LifePlanDTO currentPlan = lifePlanQueryApi.getCurrentPlanByUserId(userId);
        List<CheckinRecord> records = listRecords(userId, startDate, endDate);
        AnalysisStats stats = calculateStats(records, normalizedPeriod, startDate, endDate);
        Map<String, Object> inputs = buildInputs(userId, currentPlan, records, stats);
        String inputSummary = buildInputSummary(inputs, stats);

        try {
            String rawResponse = difyService.callCheckinAnalysis(inputs, String.valueOf(userId));
            JsonNode analysisJson = extractAnalysisJson(rawResponse);
            CheckinAnalysis analysis = buildSuccessAnalysis(userId, currentPlan, stats, inputSummary, analysisJson);
            checkinAnalysisMapper.insert(analysis);
            saveCallLog(userId, inputSummary, analysis.getSummary(), CALL_STATUS_SUCCESS, null);
            return toResponse(analysis);
        } catch (Exception exception) {
            CheckinAnalysis failed = buildFailedAnalysis(userId, currentPlan, stats, inputSummary, exception);
            checkinAnalysisMapper.insert(failed);
            saveCallLog(userId, inputSummary, failed.getSummary(), CALL_STATUS_FAILED, failed.getErrorMessage());
            return toResponse(failed);
        }
    }

    @Override
    public CheckinAnalysisResponse getLatestAnalysis(Integer userId) {
        CheckinAnalysis analysis = checkinAnalysisMapper.selectOne(new LambdaQueryWrapper<CheckinAnalysis>()
                .eq(CheckinAnalysis::getUserId, userId)
                .orderByDesc(CheckinAnalysis::getCreateTime)
                .last("LIMIT 1"));
        if (analysis == null) {
            return null;
        }
        return toResponse(analysis);
    }

    @Override
    public PageResult<CheckinAnalysisResponse> listHistory(Integer userId, Integer page, Integer pageSize,
                                                           LocalDate startDate, LocalDate endDate) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        LambdaQueryWrapper<CheckinAnalysis> wrapper = new LambdaQueryWrapper<CheckinAnalysis>()
                .eq(CheckinAnalysis::getUserId, userId)
                .ge(startDate != null, CheckinAnalysis::getStartDate, startDate)
                .le(endDate != null, CheckinAnalysis::getEndDate, endDate)
                .orderByDesc(CheckinAnalysis::getCreateTime);
        Page<CheckinAnalysis> result = checkinAnalysisMapper.selectPage(Page.of(normalizedPage, normalizedPageSize), wrapper);
        List<CheckinAnalysisResponse> list = result.getRecords().stream().map(this::toResponse).toList();
        return new PageResult<>(list, result.getTotal(), normalizedPage, normalizedPageSize);
    }

    private List<CheckinRecord> listRecords(Integer userId, LocalDate startDate, LocalDate endDate) {
        return checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .ge(CheckinRecord::getCheckinDate, startDate)
                .le(CheckinRecord::getCheckinDate, endDate)
                .orderByAsc(CheckinRecord::getCheckinDate)
                .orderByAsc(CheckinRecord::getTaskType));
    }

    private AnalysisStats calculateStats(List<CheckinRecord> records, int period, LocalDate startDate, LocalDate endDate) {
        long dietCompletionCount = records.stream()
                .filter(record -> TASK_TYPE_DIET.equals(record.getTaskType()))
                .filter(record -> STATUS_COMPLETED.equals(record.getStatus()))
                .map(CheckinRecord::getCheckinDate)
                .distinct()
                .count();
        long exerciseCompletionCount = records.stream()
                .filter(record -> TASK_TYPE_EXERCISE.equals(record.getTaskType()))
                .filter(record -> STATUS_COMPLETED.equals(record.getStatus()))
                .map(CheckinRecord::getCheckinDate)
                .distinct()
                .count();
        int totalTaskCount = records.size();
        int completedTaskCount = (int) records.stream()
                .filter(record -> STATUS_COMPLETED.equals(record.getStatus()))
                .count();
        BigDecimal completionRate = calculateCompletionRate(completedTaskCount, totalTaskCount);
        return new AnalysisStats(period, startDate, endDate, (int) dietCompletionCount, (int) exerciseCompletionCount,
                totalTaskCount, completedTaskCount, completionRate);
    }

    private Map<String, Object> buildInputs(Integer userId, LifePlanDTO currentPlan, List<CheckinRecord> records,
                                            AnalysisStats stats) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("user_profile", safeSummary(() -> patientProfileQueryApi.getProfileSummaryByUserId(userId)));
        inputs.put("latest_health_data", safeSummary(() -> healthMetricQueryApi.getLatestMetricSummaryByUserId(userId)));
        inputs.put("risk_result", safeSummary(() -> riskAssessmentQueryApi.getLatestRiskSummaryByUserId(userId)));
        inputs.put("life_plan", currentPlan == null ? "No active life plan" : blankToDefault(currentPlan.getSummary()));
        inputs.put("checkin_records", toJson(buildDifyRecords(records)));
        inputs.put("diet_completion_count", stats.dietCompletionCount());
        inputs.put("exercise_completion_count", stats.exerciseCompletionCount());
        inputs.put("total_days", stats.totalDays());
        inputs.put("completion_rate", stats.completionRate());
        inputs.put("start_date", stats.startDate().toString());
        inputs.put("end_date", stats.endDate().toString());
        inputs.put("user_notes", buildUserNotes(records));
        return inputs;
    }

    private List<Map<String, Object>> buildDifyRecords(List<CheckinRecord> records) {
        Map<LocalDate, Map<String, Object>> byDate = new LinkedHashMap<>();
        for (CheckinRecord record : records) {
            Map<String, Object> item = byDate.computeIfAbsent(record.getCheckinDate(), date -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("date", date.toString());
                value.put("diet", "");
                value.put("exercise", "");
                value.put("note", "");
                return value;
            });
            if (TASK_TYPE_DIET.equals(record.getTaskType()) || TASK_TYPE_EXERCISE.equals(record.getTaskType())) {
                item.put(record.getTaskType(), record.getStatus());
            }
            if (StringUtils.hasText(record.getNote())) {
                String existing = Objects.toString(item.get("note"), "");
                item.put("note", StringUtils.hasText(existing) ? existing + "; " + record.getNote() : record.getNote());
            }
        }
        return new ArrayList<>(byDate.values());
    }

    private String buildUserNotes(List<CheckinRecord> records) {
        return records.stream()
                .map(CheckinRecord::getNote)
                .filter(StringUtils::hasText)
                .distinct()
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
    }

    private JsonNode extractAnalysisJson(String rawResponse) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode outputs = root.path("data").path("outputs");
        JsonNode analysisResult = outputs.path("analysis_result");
        if (!analysisResult.isMissingNode() && !analysisResult.isNull()) {
            if (analysisResult.isTextual()) {
                return objectMapper.readTree(extractJsonObjectText(analysisResult.asText()));
            }
            return analysisResult;
        }
        JsonNode answer = root.path("data").path("answer");
        if (!answer.isMissingNode() && answer.isTextual()) {
            return objectMapper.readTree(extractJsonObjectText(answer.asText()));
        }
        JsonNode topAnswer = root.path("answer");
        if (!topAnswer.isMissingNode() && topAnswer.isTextual()) {
            return objectMapper.readTree(extractJsonObjectText(topAnswer.asText()));
        }
        if (!outputs.isMissingNode() && outputs.isObject()) {
            return outputs;
        }
        if (root.has("completion_rate") || root.has("summary")) {
            return root;
        }
        throw new BusinessException("Dify response does not contain analysis JSON");
    }

    private String extractJsonObjectText(String text) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("Dify analysis result is empty");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException("Dify analysis result is not JSON");
        }
        return text.substring(start, end + 1);
    }

    private CheckinAnalysis buildSuccessAnalysis(Integer userId, LifePlanDTO currentPlan, AnalysisStats stats,
                                                 String inputSummary, JsonNode analysisJson) {
        CheckinAnalysis analysis = baseAnalysis(userId, currentPlan, stats, inputSummary);
        analysis.setHabitScore(readInt(analysisJson, "habit_score"));
        analysis.setDietSummary(readText(analysisJson, "diet_summary"));
        analysis.setExerciseSummary(readText(analysisJson, "exercise_summary"));
        analysis.setLifeEvaluation(readText(analysisJson, "life_evaluation"));
        analysis.setMainProblems(toJsonArrayString(analysisJson.path("main_problems")));
        analysis.setImprovementSuggestions(toJsonArrayString(analysisJson.path("improvement_suggestions")));
        analysis.setNextFocus(readText(analysisJson, "next_focus"));
        analysis.setSummary(readText(analysisJson, "summary"));
        analysis.setCallStatus(CALL_STATUS_SUCCESS);
        analysis.setErrorMessage(null);
        return analysis;
    }

    private CheckinAnalysis buildFailedAnalysis(Integer userId, LifePlanDTO currentPlan, AnalysisStats stats,
                                                String inputSummary, Exception exception) {
        CheckinAnalysis analysis = baseAnalysis(userId, currentPlan, stats, inputSummary);
        analysis.setCallStatus(CALL_STATUS_FAILED);
        analysis.setErrorMessage(truncate(exception.getMessage(), 1000));
        analysis.setSummary("Dify check-in analysis failed. Please retry after configuration or network checks.");
        analysis.setMainProblems("[]");
        analysis.setImprovementSuggestions("[]");
        return analysis;
    }

    private CheckinAnalysis baseAnalysis(Integer userId, LifePlanDTO currentPlan, AnalysisStats stats, String inputSummary) {
        CheckinAnalysis analysis = new CheckinAnalysis();
        analysis.setUserId(userId);
        analysis.setPlanId(currentPlan == null ? null : currentPlan.getPlanId());
        analysis.setStartDate(stats.startDate());
        analysis.setEndDate(stats.endDate());
        analysis.setTotalDays(stats.totalDays());
        analysis.setDietCompletionCount(stats.dietCompletionCount());
        analysis.setExerciseCompletionCount(stats.exerciseCompletionCount());
        analysis.setCompletionRate(stats.completionRate());
        analysis.setInputSummary(inputSummary);
        analysis.setCreateTime(LocalDateTime.now());
        return analysis;
    }

    private CheckinAnalysisResponse toResponse(CheckinAnalysis analysis) {
        CheckinAnalysisResponse response = new CheckinAnalysisResponse();
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
        response.setMainProblems(parseArray(analysis.getMainProblems()));
        response.setImprovementSuggestions(parseArray(analysis.getImprovementSuggestions()));
        response.setNextFocus(analysis.getNextFocus());
        response.setSummary(analysis.getSummary());
        response.setInputSummary(analysis.getInputSummary());
        response.setCallStatus(analysis.getCallStatus());
        response.setErrorMessage(analysis.getErrorMessage());
        response.setCreateTime(analysis.getCreateTime());
        return response;
    }

    private BigDecimal calculateCompletionRate(int completedTaskCount, int totalTaskCount) {
        if (totalTaskCount == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(completedTaskCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalTaskCount), 2, RoundingMode.HALF_UP);
    }

    private int normalizePeriod(Integer period) {
        return period != null && period == 30 ? 30 : 7;
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

    private String safeSummary(SummarySupplier supplier) {
        try {
            return blankToDefault(supplier.get());
        } catch (Exception exception) {
            return "No data";
        }
    }

    private String blankToDefault(String value) {
        return StringUtils.hasText(value) ? value : "No data";
    }

    private String buildInputSummary(Map<String, Object> inputs, AnalysisStats stats) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("start_date", stats.startDate().toString());
        summary.put("end_date", stats.endDate().toString());
        summary.put("total_days", stats.totalDays());
        summary.put("diet_completion_count", stats.dietCompletionCount());
        summary.put("exercise_completion_count", stats.exerciseCompletionCount());
        summary.put("total_task_count", stats.totalTaskCount());
        summary.put("completed_task_count", stats.completedTaskCount());
        summary.put("completion_rate", stats.completionRate());
        summary.put("life_plan", inputs.get("life_plan"));
        summary.put("user_notes", inputs.get("user_notes"));
        return toJson(summary);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String toJsonArrayString(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "[]";
        }
        if (node.isArray()) {
            return node.toString();
        }
        if (node.isTextual() && StringUtils.hasText(node.asText())) {
            try {
                JsonNode parsed = objectMapper.readTree(node.asText());
                return parsed.isArray() ? parsed.toString() : objectMapper.writeValueAsString(List.of(node.asText()));
            } catch (Exception exception) {
                return toJson(List.of(node.asText()));
            }
        }
        return "[]";
    }

    private List<String> parseArray(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String readText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private Integer readInt(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asInt() : null;
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "Unknown Dify call failure";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void saveCallLog(Integer userId, String requestSummary, String responseSummary, String callStatus,
                             String errorMessage) {
        ApiCallLog log = new ApiCallLog();
        log.setUserId(userId);
        log.setServiceType(SERVICE_CHECKIN_ANALYSIS);
        log.setRequestSummary(truncate(requestSummary, 2000));
        log.setResponseSummary(truncate(responseSummary, 2000));
        log.setCallStatus(callStatus);
        log.setErrorMessage(truncate(errorMessage, 1000));
        log.setCreateTime(LocalDateTime.now());
        apiCallLogMapper.insert(log);
    }

    @FunctionalInterface
    private interface SummarySupplier {
        String get();
    }

    private record AnalysisStats(int totalDays, LocalDate startDate, LocalDate endDate, int dietCompletionCount,
                                 int exerciseCompletionCount, int totalTaskCount, int completedTaskCount,
                                 BigDecimal completionRate) {
    }
}
