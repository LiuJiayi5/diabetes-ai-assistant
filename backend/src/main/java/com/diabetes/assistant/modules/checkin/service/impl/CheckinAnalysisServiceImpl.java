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
            CheckinAnalysis fallback = buildLocalFallbackAnalysis(userId, currentPlan, records, stats, inputSummary, exception);
            checkinAnalysisMapper.insert(fallback);
            saveCallLog(userId, inputSummary, "Dify failed; local check-in analysis generated. "
                    + failureMessage(exception), CALL_STATUS_SUCCESS, null);
            return toResponse(fallback);
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
        inputs.put("user_id", String.valueOf(userId));
        inputs.put("user_profile", safeSummary(() -> patientProfileQueryApi.getProfileSummaryByUserId(userId)));
        inputs.put("latest_health_data", safeSummary(() -> healthMetricQueryApi.getLatestMetricSummaryByUserId(userId)));
        inputs.put("risk_result", safeSummary(() -> riskAssessmentQueryApi.getLatestRiskSummaryByUserId(userId)));
        inputs.put("life_plan", currentPlan == null ? "No active life plan" : blankToDefault(currentPlan.getSummary()));
        inputs.put("plan_title", currentPlan == null ? "" : blankToDefault(currentPlan.getPlanTitle()));
        inputs.put("plan_goal", currentPlan == null ? "" : blankToDefault(currentPlan.getPlanGoal()));
        inputs.put("plan_tasks_json", currentPlan == null ? "[]" : blankToDefault(currentPlan.getCheckinTasksJson()));
        inputs.put("checkin_records", toJson(buildDifyRecords(records)));
        inputs.put("checkin_stats_json", toJson(buildCheckinStats(stats)));
        inputs.put("checkin_trend_json", toJson(buildCheckinTrend(records, stats)));
        inputs.put("task_breakdown_json", toJson(buildTaskBreakdown(records)));
        inputs.put("diet_completion_count", stats.dietCompletionCount());
        inputs.put("exercise_completion_count", stats.exerciseCompletionCount());
        inputs.put("total_days", stats.totalDays());
        inputs.put("completion_rate", stats.completionRate());
        inputs.put("start_date", stats.startDate().toString());
        inputs.put("end_date", stats.endDate().toString());
        inputs.put("user_notes", buildUserNotes(records));
        inputs.put("safety_rules", buildSafetyRules());
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

    private Map<String, Object> buildCheckinStats(AnalysisStats stats) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("start_date", stats.startDate().toString());
        value.put("end_date", stats.endDate().toString());
        value.put("total_days", stats.totalDays());
        value.put("diet_completion_count", stats.dietCompletionCount());
        value.put("exercise_completion_count", stats.exerciseCompletionCount());
        value.put("total_task_count", stats.totalTaskCount());
        value.put("completed_task_count", stats.completedTaskCount());
        value.put("unfinished_task_count", Math.max(0, stats.totalTaskCount() - stats.completedTaskCount()));
        value.put("completion_rate", stats.completionRate());
        value.put("diet_completion_rate", calculateCompletionRate(stats.dietCompletionCount(), stats.totalDays()));
        value.put("exercise_completion_rate", calculateCompletionRate(stats.exerciseCompletionCount(), stats.totalDays()));
        value.put("data_sufficiency", stats.totalDays() >= 3 && stats.totalTaskCount() >= 3 ? "enough" : "insufficient");
        return value;
    }

    private List<Map<String, Object>> buildCheckinTrend(List<CheckinRecord> records, AnalysisStats stats) {
        Map<LocalDate, DailyStatus> byDate = new LinkedHashMap<>();
        for (int offset = 0; offset < stats.totalDays(); offset++) {
            LocalDate date = stats.startDate().plusDays(offset);
            byDate.put(date, new DailyStatus(date));
        }
        for (CheckinRecord record : records) {
            DailyStatus status = byDate.computeIfAbsent(record.getCheckinDate(), DailyStatus::new);
            if (TASK_TYPE_DIET.equals(record.getTaskType())) {
                status.dietStatus = record.getStatus();
            } else if (TASK_TYPE_EXERCISE.equals(record.getTaskType())) {
                status.exerciseStatus = record.getStatus();
            }
            if (StringUtils.hasText(record.getNote())) {
                status.notes.add(record.getNote());
            }
        }
        return byDate.values().stream().map(status -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", status.date.toString());
            item.put("diet_status", blankToDefault(status.dietStatus));
            item.put("exercise_status", blankToDefault(status.exerciseStatus));
            item.put("completed_count", status.completedCount());
            item.put("missed_count", status.missedCount());
            item.put("notes", status.notes);
            return item;
        }).toList();
    }

    private Map<String, Object> buildTaskBreakdown(List<CheckinRecord> records) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("diet", buildTaskTypeBreakdown(records, TASK_TYPE_DIET));
        value.put("exercise", buildTaskTypeBreakdown(records, TASK_TYPE_EXERCISE));
        return value;
    }

    private Map<String, Object> buildTaskTypeBreakdown(List<CheckinRecord> records, String taskType) {
        List<CheckinRecord> filtered = records.stream()
                .filter(record -> taskType.equals(record.getTaskType()))
                .toList();
        long completed = filtered.stream().filter(record -> STATUS_COMPLETED.equals(record.getStatus())).count();
        long pending = filtered.stream().filter(record -> "pending".equals(record.getStatus())).count();
        long missed = filtered.stream().filter(record -> "missed".equals(record.getStatus())).count();
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("total", filtered.size());
        value.put("completed", completed);
        value.put("pending", pending);
        value.put("missed", missed);
        value.put("task_names", filtered.stream()
                .map(CheckinRecord::getTaskName)
                .filter(StringUtils::hasText)
                .distinct()
                .toList());
        return value;
    }

    private String buildSafetyRules() {
        if (System.currentTimeMillis() >= 0) {
            return "\u4ec5\u505a\u751f\u6d3b\u65b9\u5f0f\u548c\u6253\u5361\u884c\u4e3a\u5206\u6790\uff0c\u4e0d\u8f93\u51fa\u8bca\u65ad\u6216\u5904\u65b9\uff1b\n"
                    + "\u5982\u51fa\u73b0\u6301\u7eed\u9ad8\u8840\u7cd6\u3001\u4f4e\u8840\u7cd6\u75c7\u72b6\u3001\u80f8\u95f7\u80f8\u75db\u3001\u660e\u663e\u4e0d\u9002\u6216\u6307\u6807\u663e\u8457\u5f02\u5e38\uff0c\u5e94\u5efa\u8bae\u7ebf\u4e0b\u5c31\u533b\u6216\u590d\u67e5\uff1b\n"
                    + "\u5efa\u8bae\u5fc5\u987b\u5177\u4f53\u3001\u6e29\u548c\u3001\u53ef\u6267\u884c\uff0c\u5e76\u7ed3\u5408\u7528\u6237\u5f53\u524d\u751f\u6d3b\u65b9\u6848\u76ee\u6807\uff1b\n"
                    + "\u6570\u636e\u4e0d\u8db3\u65f6\u8981\u660e\u786e\u8bf4\u660e\u5206\u6790\u4ec5\u4f9b\u53c2\u8003\uff0c\u4e0d\u80fd\u8fc7\u5ea6\u4e0b\u7ed3\u8bba\u3002";
        }
        return """
                仅做生活方式和打卡行为分析，不输出诊断或处方；
                如出现持续高血糖、低血糖症状、胸闷胸痛、明显不适或指标显著异常，应建议线下就医或复查；
                建议必须具体、温和、可执行，并结合用户当前生活方案目标；
                数据不足时要明确说明分析仅供参考，不能过度下结论。
                """.trim();
    }

    private JsonNode extractAnalysisJson(String rawResponse) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode outputs = root.path("data").path("outputs");
        JsonNode success = outputs.path("success");
        if (success.isBoolean() && !success.asBoolean()) {
            String message = outputs.path("error_message").asText("Dify check-in analysis workflow returned failure");
            throw new BusinessException(message);
        }
        JsonNode analysisResult = outputs.path("analysis_result");
        if (!analysisResult.isMissingNode() && !analysisResult.isNull()) {
            if (analysisResult.isTextual()) {
                JsonNode parsed = objectMapper.readTree(extractJsonObjectText(analysisResult.asText()));
                validateAnalysisJson(parsed);
                return parsed;
            }
            validateAnalysisJson(analysisResult);
            return analysisResult;
        }
        JsonNode answer = root.path("data").path("answer");
        if (!answer.isMissingNode() && answer.isTextual()) {
            JsonNode parsed = objectMapper.readTree(extractJsonObjectText(answer.asText()));
            validateAnalysisJson(parsed);
            return parsed;
        }
        JsonNode topAnswer = root.path("answer");
        if (!topAnswer.isMissingNode() && topAnswer.isTextual()) {
            JsonNode parsed = objectMapper.readTree(extractJsonObjectText(topAnswer.asText()));
            validateAnalysisJson(parsed);
            return parsed;
        }
        if (!outputs.isMissingNode() && outputs.isObject()) {
            throw new BusinessException(buildMissingAnalysisMessage(outputs));
        }
        if (root.isObject()) {
            validateAnalysisJson(root);
            return root;
        }
        throw new BusinessException("Dify response does not contain check-in analysis JSON");
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

    private void validateAnalysisJson(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new BusinessException("Dify check-in analysis result is not a JSON object");
        }
        List<String> missing = new ArrayList<>();
        requireText(node, "diet_summary", missing);
        requireText(node, "exercise_summary", missing);
        requireText(node, "life_evaluation", missing);
        requireText(node, "next_focus", missing);
        requireText(node, "summary", missing);
        if (!node.path("habit_score").isNumber()) {
            missing.add("habit_score");
        }
        if (!node.path("main_problems").isArray() || node.path("main_problems").isEmpty()) {
            missing.add("main_problems");
        }
        if (!node.path("improvement_suggestions").isArray() || node.path("improvement_suggestions").isEmpty()) {
            missing.add("improvement_suggestions");
        }
        if (!missing.isEmpty()) {
            throw new BusinessException("Dify check-in analysis output missing required fields: "
                    + String.join(", ", missing));
        }
    }

    private void requireText(JsonNode node, String field, List<String> missing) {
        if (!StringUtils.hasText(node.path(field).asText(""))) {
            missing.add(field);
        }
    }

    private String buildMissingAnalysisMessage(JsonNode outputs) {
        String errorMessage = outputs.path("error_message").asText("");
        if (StringUtils.hasText(errorMessage)) {
            return "Dify check-in analysis did not return analysis_result: " + errorMessage;
        }
        List<String> fields = new ArrayList<>();
        outputs.fieldNames().forEachRemaining(fields::add);
        return "Dify check-in analysis did not return analysis_result. Output fields: "
                + (fields.isEmpty() ? "none" : String.join(", ", fields));
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
        analysis.setErrorMessage(truncate(failureMessage(exception), 1000));
        analysis.setSummary("打卡分析生成失败：" + analysis.getErrorMessage());
        analysis.setMainProblems("[]");
        analysis.setImprovementSuggestions("[]");
        return analysis;
    }

    private CheckinAnalysis buildLocalFallbackAnalysis(Integer userId, LifePlanDTO currentPlan, List<CheckinRecord> records,
                                                       AnalysisStats stats, String inputSummary, Exception exception) {
        CheckinAnalysis analysis = baseAnalysis(userId, currentPlan, stats, inputSummary);
        int dietRate = stats.totalDays() == 0 ? 0 : stats.dietCompletionCount() * 100 / stats.totalDays();
        int exerciseRate = stats.totalDays() == 0 ? 0 : stats.exerciseCompletionCount() * 100 / stats.totalDays();
        int score = calculateHabitScore(stats, dietRate, exerciseRate);

        analysis.setHabitScore(score);
        analysis.setDietSummary(buildDietSummary(stats, dietRate));
        analysis.setExerciseSummary(buildExerciseSummary(stats, exerciseRate));
        analysis.setLifeEvaluation(buildLifeEvaluation(currentPlan, stats, score));
        analysis.setMainProblems(toJson(buildMainProblems(stats, dietRate, exerciseRate)));
        analysis.setImprovementSuggestions(toJson(buildImprovementSuggestions(records, stats, dietRate, exerciseRate)));
        analysis.setNextFocus(buildNextFocus(dietRate, exerciseRate));
        analysis.setSummary(buildFallbackSummary(stats, score));
        analysis.setCallStatus(CALL_STATUS_SUCCESS);
        analysis.setErrorMessage(null);
        return analysis;
    }

    private int calculateHabitScore(AnalysisStats stats, int dietRate, int exerciseRate) {
        int base = stats.completionRate().setScale(0, RoundingMode.HALF_UP).intValue();
        int balancePenalty = Math.min(12, Math.abs(dietRate - exerciseRate) / 5);
        int dataBonus = stats.totalTaskCount() >= stats.totalDays() * 2 ? 4 : 0;
        return Math.max(40, Math.min(95, base - balancePenalty + dataBonus));
    }

    private String buildDietSummary(AnalysisStats stats, int dietRate) {
        if (stats.dietCompletionCount() == 0) {
            return "本周期尚未完成饮食打卡，建议先保证每天记录主食、蔬菜、蛋白质和加餐情况。";
        }
        if (dietRate >= 85) {
            return "本周期饮食打卡完成 " + stats.dietCompletionCount() + " 天，连续性较好，适合继续巩固控糖餐盘和少油少糖习惯。";
        }
        if (dietRate >= 60) {
            return "本周期饮食打卡完成 " + stats.dietCompletionCount() + " 天，已有一定基础，但仍有漏记或执行不稳定的日期。";
        }
        return "本周期饮食打卡完成率偏低，需要先把记录习惯建立起来，再逐步优化晚餐主食和外卖选择。";
    }

    private String buildExerciseSummary(AnalysisStats stats, int exerciseRate) {
        if (stats.exerciseCompletionCount() == 0) {
            return "本周期尚未完成运动打卡，建议从饭后轻松步行 10 到 15 分钟开始，避免突然增加强度。";
        }
        if (exerciseRate >= 85) {
            return "本周期运动打卡完成 " + stats.exerciseCompletionCount() + " 天，饭后活动保持较好，可以继续关注运动后的身体感受。";
        }
        if (exerciseRate >= 60) {
            return "本周期运动打卡完成 " + stats.exerciseCompletionCount() + " 天，整体可继续推进，薄弱点主要是运动连续性。";
        }
        return "本周期运动打卡完成率偏低，建议优先安排固定的餐后步行时段，并从舒适强度开始。";
    }

    private String buildLifeEvaluation(LifePlanDTO currentPlan, AnalysisStats stats, int score) {
        String goal = currentPlan == null ? "" : currentPlan.getPlanGoal();
        String goalText = StringUtils.hasText(goal) ? "当前方案目标是“" + goal + "”。" : "";
        if (score >= 80) {
            return goalText + "本周期饮食和运动执行较稳定，已经具备继续巩固生活方案的基础。";
        }
        if (score >= 60) {
            return goalText + "本周期已有一定执行基础，但饮食和运动完成度还不够均衡，需要继续提升连续性。";
        }
        return goalText + "本周期打卡和方案执行仍偏弱，建议先降低执行门槛，把每天记录和轻量行动稳定下来。";
    }

    private List<String> buildMainProblems(AnalysisStats stats, int dietRate, int exerciseRate) {
        List<String> problems = new ArrayList<>();
        if (stats.totalTaskCount() < stats.totalDays() * 2) {
            problems.add("部分日期打卡记录不完整，后续分析的准确性会受到影响");
        }
        if (dietRate < 80) {
            problems.add("饮食打卡连续性不足，容易遗漏晚餐主食、外卖或加餐等关键行为");
        }
        if (exerciseRate < 80) {
            problems.add("运动打卡仍不够稳定，餐后活动频率需要继续提高");
        }
        if (Math.abs(dietRate - exerciseRate) >= 20) {
            problems.add("饮食与运动执行不够均衡，容易出现某一项坚持、另一项掉队的情况");
        }
        if (problems.isEmpty()) {
            problems.add("当前执行情况较好，但仍需关注周末或天气变化时的打卡稳定性");
            problems.add("建议继续观察完成率变化，避免短期达标后放松记录");
        }
        while (problems.size() < 2) {
            problems.add("打卡数据仍需继续积累，便于判断习惯是否真正稳定");
        }
        return problems.size() > 4 ? problems.subList(0, 4) : problems;
    }

    private List<String> buildImprovementSuggestions(List<CheckinRecord> records, AnalysisStats stats,
                                                     int dietRate, int exerciseRate) {
        List<String> suggestions = new ArrayList<>();
        if (dietRate < 85) {
            suggestions.add("接下来一周优先保证每天完成饮食打卡，晚餐主食控制在平时的三分之二，并搭配蔬菜和优质蛋白");
        } else {
            suggestions.add("继续保持饮食记录，重点观察外卖、加餐和含糖饮料是否影响第二天状态");
        }
        if (exerciseRate < 85) {
            suggestions.add("把饭后 15 到 20 分钟轻松步行设为固定任务，如遇下雨可改为室内拉伸或原地踏步");
        } else {
            suggestions.add("继续保持饭后活动，运动时以能正常说话、不明显气喘为宜");
        }
        suggestions.add("每天打卡时补一句身体感受或饮食变化，方便后续判断血糖波动和生活习惯之间的关系");
        if (hasRecentNotes(records)) {
            suggestions.add("保留现有备注习惯，遇到漏打卡时记录原因，例如外出、天气、加班或身体不适");
        }
        suggestions.add("如果出现明显不适、疑似低血糖或血糖持续异常，应及时线下复查或咨询医生");
        return suggestions.size() > 5 ? suggestions.subList(0, 5) : suggestions;
    }

    private boolean hasRecentNotes(List<CheckinRecord> records) {
        return records.stream().anyMatch(record -> StringUtils.hasText(record.getNote()));
    }

    private String buildNextFocus(int dietRate, int exerciseRate) {
        if (dietRate < exerciseRate) {
            return "下一阶段优先补齐饮食记录，重点关注晚餐主食、外卖选择和加餐情况。";
        }
        if (exerciseRate < dietRate) {
            return "下一阶段优先提升运动连续性，把餐后轻量步行固定为每天最小行动。";
        }
        return "下一阶段继续保持饮食与运动同步打卡，观察完成率和身体感受的变化。";
    }

    private String buildFallbackSummary(AnalysisStats stats, int score) {
        return "本周期完成率 " + stats.completionRate() + "%，习惯评分 " + score + "，建议继续提升饮食与运动打卡的连续性。";
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
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String failureMessage(Exception exception) {
        if (exception == null || !StringUtils.hasText(exception.getMessage())) {
            return "Unknown Dify call failure";
        }
        return exception.getMessage();
    }

    private void saveCallLog(Integer userId, String requestSummary, String responseSummary, String callStatus,
                             String errorMessage) {
        ApiCallLog log = new ApiCallLog();
        log.setUserId(userId);
        log.setServiceType(SERVICE_CHECKIN_ANALYSIS);
        log.setRequestSummary(truncate(requestSummary, 2000));
        log.setResponseSummary(truncate(responseSummary, 2000));
        log.setCallStatus(callStatus);
        log.setErrorMessage(StringUtils.hasText(errorMessage) ? truncate(errorMessage, 1000) : null);
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

    private static final class DailyStatus {
        private final LocalDate date;
        private String dietStatus = "";
        private String exerciseStatus = "";
        private final List<String> notes = new ArrayList<>();

        private DailyStatus(LocalDate date) {
            this.date = date;
        }

        private int completedCount() {
            int count = 0;
            if (STATUS_COMPLETED.equals(dietStatus)) {
                count++;
            }
            if (STATUS_COMPLETED.equals(exerciseStatus)) {
                count++;
            }
            return count;
        }

        private int missedCount() {
            int count = 0;
            if ("missed".equals(dietStatus) || "pending".equals(dietStatus)) {
                count++;
            }
            if ("missed".equals(exerciseStatus) || "pending".equals(exerciseStatus)) {
                count++;
            }
            return count;
        }
    }
}
