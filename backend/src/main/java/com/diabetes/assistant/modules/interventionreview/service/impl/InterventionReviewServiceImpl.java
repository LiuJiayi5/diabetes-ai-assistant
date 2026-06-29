package com.diabetes.assistant.modules.interventionreview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.constants.RoleConstants;
import com.diabetes.assistant.common.constants.StatusConstants;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.checkin.contract.CheckinQueryApi;
import com.diabetes.assistant.modules.checkin.contract.dto.CheckinAnalysisDTO;
import com.diabetes.assistant.modules.checkin.contract.dto.CheckinRecordDTO;
import com.diabetes.assistant.modules.dify.dto.DifyWorkflowResult;
import com.diabetes.assistant.modules.dify.service.DifyService;
import com.diabetes.assistant.modules.healthmetric.contract.HealthMetricQueryApi;
import com.diabetes.assistant.modules.healthmetric.contract.dto.HealthMetricDTO;
import com.diabetes.assistant.modules.interventionreview.dto.InterventionReviewResponse;
import com.diabetes.assistant.modules.interventionreview.entity.InterventionReview;
import com.diabetes.assistant.modules.interventionreview.mapper.InterventionReviewMapper;
import com.diabetes.assistant.modules.interventionreview.service.InterventionReviewService;
import com.diabetes.assistant.modules.lifeplan.contract.LifePlanQueryApi;
import com.diabetes.assistant.modules.lifeplan.contract.dto.LifePlanDTO;
import com.diabetes.assistant.modules.lifeplan.entity.LifePlan;
import com.diabetes.assistant.modules.lifeplan.mapper.LifePlanMapper;
import com.diabetes.assistant.modules.profile.contract.PatientProfileQueryApi;
import com.diabetes.assistant.modules.profile.contract.dto.PatientProfileDTO;
import com.diabetes.assistant.modules.risk.contract.RiskAssessmentQueryApi;
import com.diabetes.assistant.modules.risk.contract.dto.RiskAssessmentDTO;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import com.diabetes.assistant.modules.user.contract.dto.UserBasicDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterventionReviewServiceImpl implements InterventionReviewService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_HISTORY = "history";
    private static final String CALL_SUCCESS = "success";
    private static final String CALL_FAILED = "failed";
    private static final int REVIEW_DAYS = 7;

    private final InterventionReviewMapper interventionReviewMapper;
    private final LifePlanMapper lifePlanMapper;
    private final LifePlanQueryApi lifePlanQueryApi;
    private final CheckinQueryApi checkinQueryApi;
    private final PatientProfileQueryApi patientProfileQueryApi;
    private final HealthMetricQueryApi healthMetricQueryApi;
    private final RiskAssessmentQueryApi riskAssessmentQueryApi;
    private final DifyService difyService;
    private final UserQueryApi userQueryApi;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void tryAutoReview(Integer userId, String triggerType, String triggerReason) {
        try {
            if (userId == null || !StringUtils.hasText(triggerType)) {
                return;
            }
            LifePlanDTO currentPlanDto = lifePlanQueryApi.getCurrentPlanByUserId(userId);
            if (currentPlanDto == null || currentPlanDto.getPlanId() == null || currentPlanDto.getCreateTime() == null) {
                return;
            }
            PlanProgress progress = buildPlanProgress(currentPlanDto);
            if (!shouldTrigger(userId, currentPlanDto.getPlanId(), triggerType, progress)) {
                return;
            }

            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(REVIEW_DAYS - 1L);
            List<CheckinRecordDTO> records = checkinQueryApi.listRecentCheckins(userId, REVIEW_DAYS);
            CheckinStats stats = calculateStats(records, REVIEW_DAYS, startDate, endDate);
            SafetyAssessment safetyAssessment = assessSafety(records);
            Map<String, Object> inputs = buildInputs(userId, triggerType, triggerReason, currentPlanDto, progress,
                    startDate, endDate, records, stats, safetyAssessment);
            DifyWorkflowResult result = difyService.callInterventionReview(inputs, "user-" + userId);
            Map<String, Object> reviewResult = enforceSafetyResult(
                    extractReviewResult(result.getOutputs()), safetyAssessment, progress);
            InterventionReview review = buildReview(userId, currentPlanDto, triggerType, triggerReason, progress,
                    startDate, endDate, inputs, reviewResult, null);
            interventionReviewMapper.insert(review);
            Integer generatedPlanId = maybeCreateAdjustedPlan(currentPlanDto, review, reviewResult);
            if (generatedPlanId != null) {
                review.setGeneratedPlanId(generatedPlanId);
                interventionReviewMapper.updateById(review);
            }
        } catch (Exception exception) {
            log.warn("Auto intervention review skipped: userId={}, triggerType={}, error={}",
                    userId, triggerType, exception.getMessage());
            saveFailedReview(userId, triggerType, triggerReason, exception);
        }
    }

    @Override
    public InterventionReviewResponse getLatest(Integer userId) {
        InterventionReview review = interventionReviewMapper.selectOne(new LambdaQueryWrapper<InterventionReview>()
                .eq(InterventionReview::getUserId, userId)
                .orderByDesc(InterventionReview::getCreateTime)
                .orderByDesc(InterventionReview::getReviewId)
                .last("LIMIT 1"));
        return review == null ? null : toResponse(review, true);
    }

    @Override
    public PageResult<InterventionReviewResponse> listHistory(Integer userId, Integer page, Integer pageSize) {
        int current = normalizePage(page);
        int size = normalizePageSize(pageSize);
        Page<InterventionReview> result = interventionReviewMapper.selectPage(Page.of(current, size),
                new LambdaQueryWrapper<InterventionReview>()
                        .eq(InterventionReview::getUserId, userId)
                        .orderByDesc(InterventionReview::getCreateTime)
                        .orderByDesc(InterventionReview::getReviewId));
        return new PageResult<>(result.getRecords().stream().map(item -> toResponse(item, true)).toList(),
                result.getTotal(), current, size);
    }

    @Override
    public PageResult<InterventionReviewResponse> listAdmin(Integer adminUserId, Integer userId, String interventionLevel,
                                                           Integer page, Integer pageSize) {
        if (!userQueryApi.isAdmin(adminUserId)) {
            throw new BusinessException(403, "No permission");
        }
        int current = normalizePage(page);
        int size = normalizePageSize(pageSize);
        Page<InterventionReview> result = interventionReviewMapper.selectPage(Page.of(current, size),
                new LambdaQueryWrapper<InterventionReview>()
                        .eq(userId != null, InterventionReview::getUserId, userId)
                        .eq(StringUtils.hasText(interventionLevel), InterventionReview::getInterventionLevel, interventionLevel)
                        .orderByDesc(InterventionReview::getCreateTime)
                        .orderByDesc(InterventionReview::getReviewId));
        return new PageResult<>(result.getRecords().stream().map(item -> toResponse(item, false)).toList(),
                result.getTotal(), current, size);
    }

    private boolean shouldTrigger(Integer userId, Integer planId, String triggerType, PlanProgress progress) {
        if ("checkin_submit".equals(triggerType)) {
            return true;
        }
        if ("health_metric_save".equals(triggerType)) {
            return true;
        }
        if ("risk_assessment_save".equals(triggerType)) {
            return true;
        }
        if ("plan_progress".equals(triggerType)) {
            return progress.planDay() == 3 || progress.planDay() == 7 || progress.expired();
        }
        return false;
    }

    private boolean hasReviewToday(Integer userId, Integer planId, String triggerType) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        Long count = interventionReviewMapper.selectCount(new LambdaQueryWrapper<InterventionReview>()
                .eq(InterventionReview::getUserId, userId)
                .eq(InterventionReview::getPlanId, planId)
                .eq(InterventionReview::getTriggerType, triggerType)
                .ge(InterventionReview::getCreateTime, start));
        return count != null && count > 0;
    }

    private PlanProgress buildPlanProgress(LifePlanDTO plan) {
        int totalDays = totalPlanDays(plan.getDailyScheduleJson());
        LocalDate startDate = plan.getCreateTime().toLocalDate();
        int planDay = (int) ChronoUnit.DAYS.between(startDate, LocalDate.now()) + 1;
        boolean expired = planDay > totalDays;
        return new PlanProgress(startDate, planDay, totalDays, expired);
    }

    private int totalPlanDays(String scheduleJson) {
        Object value = parseAny(scheduleJson);
        if (value instanceof List<?> list && !list.isEmpty()) {
            return list.size();
        }
        if (value instanceof Map<?, ?> map) {
            Object nested = firstPresent(map, "daily_schedule", "weekly_schedule", "days", "plan_days");
            if (nested instanceof List<?> list && !list.isEmpty()) {
                return list.size();
            }
        }
        return 7;
    }

    private Map<String, Object> buildInputs(Integer userId, String triggerType, String triggerReason,
                                            LifePlanDTO currentPlan, PlanProgress progress,
                                            LocalDate startDate, LocalDate endDate,
                                            List<CheckinRecordDTO> records, CheckinStats stats,
                                            SafetyAssessment safetyAssessment) {
        PatientProfileDTO profile = patientProfileQueryApi.getProfileByUserId(userId);
        HealthMetricDTO metric = healthMetricQueryApi.getLatestMetricByUserId(userId);
        RiskAssessmentDTO risk = riskAssessmentQueryApi.getLatestAssessmentByUserId(userId);
        CheckinAnalysisDTO analysis = checkinQueryApi.getLatestAnalysisByUserId(userId);

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("user_id", String.valueOf(userId));
        inputs.put("trigger_type", triggerType);
        inputs.put("trigger_reason", triggerReason);
        inputs.put("patient_profile_json", toJson(profile));
        inputs.put("latest_health_metrics_json", toJson(metric));
        inputs.put("latest_risk_assessment_json", toJson(risk));
        inputs.put("current_life_plan_json", toJson(buildPlanContext(currentPlan)));
        inputs.put("plan_progress_json", toJson(Map.of(
                "start_date", progress.startDate().toString(),
                "current_day", progress.planDay(),
                "total_days", progress.totalDays(),
                "expired", progress.expired())));
        inputs.put("recent_checkin_records_json", toJson(records));
        inputs.put("checkin_stats_json", toJson(stats.toMap()));
        inputs.put("latest_checkin_analysis_json", toJson(analysis));
        inputs.put("exercise_safety_alert_json", toJson(safetyAssessment.toMap()));
        inputs.put("plan_goal", blankToDefault(currentPlan.getPlanGoal(), ""));
        inputs.put("safety_rules", buildSafetyRules());
        inputs.put("output_contract", buildOutputContract());
        return inputs;
    }

    private Map<String, Object> buildPlanContext(LifePlanDTO plan) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("plan_id", plan.getPlanId());
        value.put("plan_title", plan.getPlanTitle());
        value.put("plan_goal", plan.getPlanGoal());
        value.put("summary", plan.getSummary());
        value.put("created_at", plan.getCreateTime() == null ? null : plan.getCreateTime().toString());
        value.put("daily_schedule", compactDailySchedule(parseAny(plan.getDailyScheduleJson())));
        value.put("checkin_tasks", compactTopLevelList(parseAny(plan.getCheckinTasksJson())));
        value.put("health_tips", compactTopLevelList(parseAny(plan.getHealthTipsJson())));
        value.put("diet_plan", compactPromptValue(parseAny(plan.getDietPlanJson())));
        value.put("exercise_plan", compactPromptValue(parseAny(plan.getExercisePlanJson())));
        return value;
    }

    private List<Object> compactDailySchedule(Object value) {
        List<Object> days = normalizeScheduleList(value);
        if (days.isEmpty()) {
            return List.of();
        }
        List<Object> compact = new ArrayList<>();
        for (Object dayValue : days) {
            Map<String, Object> dayMap = parseObject(dayValue);
            if (dayMap.isEmpty()) {
                compact.add(compactPromptValue(dayValue));
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            for (String key : List.of("day", "title", "theme", "reminder", "goal")) {
                if (dayMap.containsKey(key)) {
                    item.put(key, dayMap.get(key));
                }
            }
            for (String key : List.of("diet_plan", "exercise_plan", "meals", "tasks")) {
                if (dayMap.containsKey(key)) {
                    item.put(key, compactPromptValue(dayMap.get(key)));
                }
            }
            compact.add(item);
        }
        return compact;
    }

    private Object compactTopLevelList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(this::compactPromptValue).toList();
        }
        return compactPromptValue(value);
    }

    private Object compactPromptValue(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return truncate(toJson(value), 1200);
    }

    private String buildSafetyRules() {
        return "Only adjust lifestyle guidance, reminders, task difficulty, diet/exercise execution details, and patient-facing education. "
                + "Do not diagnose disease, prescribe medicine, change insulin dose, or replace offline medical advice. "
                + "For high-risk signals, prefer warning and offline consultation instead of aggressive automatic plan changes. "
                + "If recent check-in notes mention leg fracture, lower-limb injury, inability to walk, inability to exercise, severe leg pain, surgery, or bed rest, do not recommend walking, jogging, stair climbing, standing exercise, lower-limb stretching, original aerobic tasks, or lower-limb resistance tasks. "
                + "In that situation the plan should usually be adjusted, not blocked: pause lower-limb exercise in future days, keep diet and glucose-monitoring guidance, and only mention seated upper-body light activity or breathing relaxation when the patient has medical permission.";
    }

    private String buildOutputContract() {
        return """
                Return strict JSON in review_result:
                success:boolean, adherence_score:number, intervention_level:observe|minor_adjustment|moderate_adjustment|high_risk_alert,
                should_update_plan:boolean, update_scope:none|future_days|full_regeneration_recommended,
                affected_days:number[], main_problem_tags:string[], preserved_items:string[], changed_items:string[],
                adjustment_strategy:string, patient_notice:string, explanation:string, safety_warning:string,
                adjusted_plan_patch:{daily_schedule:[{day:number, reminder?:string, diet_plan?:object|string, exercise_plan?:object|string, meals?:object}], health_tips?:string[], summary?:string}.
                When exercise_safety_alert_json.block_lower_limb_exercise=true, adjusted_plan_patch must remove lower-limb and aerobic tasks from future days and replace them with recovery-safe reminders.
                """;
    }

    private CheckinStats calculateStats(List<CheckinRecordDTO> records, int totalDays, LocalDate startDate, LocalDate endDate) {
        int total = records == null ? 0 : records.size();
        long completed = records == null ? 0 : records.stream().filter(item -> "completed".equals(item.getStatus())).count();
        long dietCompleted = records == null ? 0 : records.stream()
                .filter(item -> "diet".equals(item.getTaskType()))
                .filter(item -> "completed".equals(item.getStatus()))
                .map(CheckinRecordDTO::getCheckinDate)
                .distinct()
                .count();
        long exerciseCompleted = records == null ? 0 : records.stream()
                .filter(item -> "exercise".equals(item.getTaskType()))
                .filter(item -> "completed".equals(item.getStatus()))
                .map(CheckinRecordDTO::getCheckinDate)
                .distinct()
                .count();
        BigDecimal completionRate = total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP);
        return new CheckinStats(totalDays, startDate, endDate, (int) dietCompleted, (int) exerciseCompleted,
                total, (int) completed, completionRate);
    }

    private SafetyAssessment assessSafety(List<CheckinRecordDTO> records) {
        if (records == null || records.isEmpty()) {
            return SafetyAssessment.empty();
        }
        List<String> keywords = List.of(
                "腿部骨折", "腿骨折", "脚骨折", "下肢骨折", "骨折",
                "扭伤", "拉伤", "摔伤", "腿疼", "脚疼", "膝盖疼",
                "不能运动", "无法运动", "一点运动都不能做", "不能走", "无法走", "走不了",
                "卧床", "手术", "术后", "行动不便");
        List<String> matchedKeywords = new ArrayList<>();
        List<String> matchedNotes = new ArrayList<>();
        for (CheckinRecordDTO record : records) {
            String note = record == null ? null : record.getNote();
            if (!StringUtils.hasText(note)) {
                continue;
            }
            boolean noteMatched = false;
            for (String keyword : keywords) {
                if (note.contains(keyword)) {
                    noteMatched = true;
                    if (!matchedKeywords.contains(keyword)) {
                        matchedKeywords.add(keyword);
                    }
                }
            }
            if (noteMatched && matchedNotes.size() < 3) {
                matchedNotes.add(note);
            }
        }
        if (matchedKeywords.isEmpty()) {
            return SafetyAssessment.empty();
        }
        String reason = "近期打卡备注提示可能存在下肢伤病或无法运动情况：" + String.join("；", matchedNotes);
        return new SafetyAssessment(true, truncate(reason, 500), matchedKeywords);
    }

    private Map<String, Object> enforceSafetyResult(Map<String, Object> original,
                                                    SafetyAssessment safetyAssessment,
                                                    PlanProgress progress) {
        if (safetyAssessment == null || !safetyAssessment.blockLowerLimbExercise()) {
            return original;
        }
        Map<String, Object> result = new LinkedHashMap<>(original == null ? Map.of() : original);
        List<Integer> affectedDays = futureAffectedDays(progress);
        result.put("success", true);
        result.put("intervention_level", "moderate_adjustment");
        result.put("should_update_plan", true);
        result.put("update_scope", "future_days");
        result.put("affected_days", affectedDays);
        result.put("main_problem_tags", appendUniqueList(result.get("main_problem_tags"),
                "lower_limb_injury", "exercise_safety_alert"));
        result.put("changed_items", appendUniqueList(result.get("changed_items"), "exercise_plan", "safety_warning"));
        result.put("adjustment_strategy", "在保留饮食与血糖管理思路的基础上，暂停步行、跑跳、下肢拉伸、原地踏步和下肢抗阻等活动；恢复期只保留安全提醒，若医生允许，可改为坐姿上肢轻活动或呼吸放松。");
        result.put("patient_notice", "已根据近期打卡中提到的腿部骨折或无法运动情况，自动把后续运动安排调整为恢复期安全版本。");
        result.put("explanation", safetyAssessment.reason());
        result.put("safety_warning", "骨折、明显疼痛或行动受限期间，请优先遵医嘱康复；不要自行进行步行、跑跳、下肢拉伸或抗阻训练。如疼痛加重、肿胀或麻木，应及时线下就医。");
        result.put("adjusted_plan_patch", buildSafetyAdjustedPatch(result.get("adjusted_plan_patch"), affectedDays));
        return result;
    }

    private List<Integer> futureAffectedDays(PlanProgress progress) {
        int totalDays = progress == null ? 7 : Math.max(1, progress.totalDays());
        int startDay = progress == null ? 1 : Math.max(1, Math.min(progress.planDay(), totalDays));
        List<Integer> days = new ArrayList<>();
        for (int day = startDay; day <= totalDays; day++) {
            days.add(day);
        }
        return days.isEmpty() ? List.of(1) : days;
    }

    private List<Object> appendUniqueList(Object source, String... values) {
        List<Object> result = new ArrayList<>();
        for (Object item : normalizeList(source)) {
            if (item != null && !result.contains(item)) {
                result.add(item);
            }
        }
        for (String value : values) {
            if (StringUtils.hasText(value) && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private Map<String, Object> buildSafetyAdjustedPatch(Object originalPatchValue, List<Integer> affectedDays) {
        Map<String, Object> originalPatch = parseObject(originalPatchValue);
        Map<Integer, Map<String, Object>> originalDays = new LinkedHashMap<>();
        Object dailySchedule = originalPatch.get("daily_schedule");
        if (dailySchedule instanceof List<?> list) {
            for (Object item : list) {
                Map<String, Object> itemMap = parseObject(item);
                Integer day = toInteger(itemMap.get("day"));
                if (day != null) {
                    originalDays.put(day, itemMap);
                }
            }
        }
        List<Map<String, Object>> safeDays = new ArrayList<>();
        for (Integer day : affectedDays) {
            Map<String, Object> safeDay = new LinkedHashMap<>(originalDays.getOrDefault(day, Map.of()));
            safeDay.put("day", day);
            safeDay.put("reminder", "今日运动目标调整为伤病恢复优先：暂停步行、跑跳、下肢拉伸和原地踏步；如医生允许，可做坐姿上肢轻活动或呼吸放松 5-10 分钟。");
            safeDay.put("exercise_plan", Map.of(
                    "light", "暂停步行、跑跳、下肢拉伸和原地踏步；如医生允许，可做坐姿上肢轻活动或呼吸放松 5-10 分钟。",
                    "aerobic", "暂不安排有氧运动，待医生确认可以恢复活动后再逐步增加。",
                    "resistance", "暂不安排下肢抗阻训练；仅在医生或康复师允许时做坐姿上肢轻活动。",
                    "notice", "骨折或行动受限期间先遵医嘱康复，任何疼痛加重都应立即停止活动。"));
            safeDays.add(safeDay);
        }
        Map<String, Object> patch = new LinkedHashMap<>(originalPatch);
        patch.put("daily_schedule", safeDays);
        patch.put("health_tips", appendUniqueList(originalPatch.get("health_tips"),
                "骨折或明显疼痛期间不要自行进行下肢运动，恢复活动前请先确认医生或康复师建议。"));
        patch.put("summary", "后续方案已根据近期伤病备注调整为恢复期安全版本：饮食和血糖管理继续执行，运动部分暂停下肢负荷活动。");
        return patch;
    }

    private Map<String, Object> extractReviewResult(Map<String, Object> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            throw new BusinessException(502, "Dify intervention review output is empty");
        }
        Object result = outputs.get("review_result");
        if (result == null) {
            result = outputs;
        }
        Map<String, Object> map = parseObject(result);
        boolean success = Boolean.TRUE.equals(map.get("success")) || "true".equalsIgnoreCase(String.valueOf(map.get("success")));
        if (!success) {
            throw new BusinessException(502, blankToDefault(asString(map.get("error_message")), "Dify intervention review failed"));
        }
        return map;
    }

    private InterventionReview buildReview(Integer userId, LifePlanDTO plan, String triggerType, String triggerReason,
                                           PlanProgress progress, LocalDate startDate, LocalDate endDate,
                                           Map<String, Object> inputs, Map<String, Object> result, Exception exception) {
        InterventionReview review = new InterventionReview();
        review.setUserId(userId);
        review.setPlanId(plan == null ? null : plan.getPlanId());
        review.setTriggerType(triggerType);
        review.setTriggerReason(triggerReason);
        review.setPeriodStartDate(startDate);
        review.setPeriodEndDate(endDate);
        review.setReviewDays(REVIEW_DAYS);
        review.setPlanDay(progress == null ? null : progress.planDay());
        review.setInputSummary(toJson(inputs));
        review.setCallStatus(exception == null ? CALL_SUCCESS : CALL_FAILED);
        review.setErrorMessage(exception == null ? null : truncate(exception.getMessage(), 1000));
        review.setCreateTime(LocalDateTime.now());
        if (result != null) {
            review.setAdherenceScore(toInteger(result.get("adherence_score")));
            review.setInterventionLevel(asString(result.get("intervention_level")));
            review.setShouldUpdatePlan(toBoolean(result.get("should_update_plan")));
            review.setUpdateScope(asString(result.get("update_scope")));
            review.setAffectedDays(toJson(normalizeList(result.get("affected_days"))));
            review.setMainProblemTags(toJson(normalizeList(result.get("main_problem_tags"))));
            review.setPreservedItems(toJson(normalizeList(result.get("preserved_items"))));
            review.setChangedItems(toJson(normalizeList(result.get("changed_items"))));
            review.setAdjustmentStrategy(asString(result.get("adjustment_strategy")));
            review.setPatientNotice(asString(result.get("patient_notice")));
            review.setExplanation(asString(result.get("explanation")));
            review.setSafetyWarning(asString(result.get("safety_warning")));
            review.setAdjustedPlanPatch(toJson(result.getOrDefault("adjusted_plan_patch", Map.of())));
            review.setRawResponse(toJson(result));
        }
        return review;
    }

    private Integer maybeCreateAdjustedPlan(LifePlanDTO currentPlanDto, InterventionReview review, Map<String, Object> result) {
        boolean shouldUpdate = Boolean.TRUE.equals(review.getShouldUpdatePlan());
        String level = blankToDefault(review.getInterventionLevel(), "");
        if (!shouldUpdate || "high_risk_alert".equals(level)) {
            return null;
        }
        Object patch = result.get("adjusted_plan_patch");
        if (patch == null || parseObject(patch).isEmpty() && !(patch instanceof Map<?, ?>)) {
            return null;
        }
        LifePlan current = lifePlanMapper.selectById(currentPlanDto.getPlanId());
        if (current == null || !StringUtils.hasText(current.getPlanJson())) {
            return null;
        }
        Map<String, Object> mergedPlan = mergePlanPatch(parseObject(current.getPlanJson()), patch);
        if (mergedPlan.isEmpty()) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        lifePlanMapper.update(null, new LambdaUpdateWrapper<LifePlan>()
                .eq(LifePlan::getPlanId, current.getPlanId())
                .eq(LifePlan::getStatus, STATUS_ACTIVE)
                .set(LifePlan::getStatus, STATUS_HISTORY)
                .set(LifePlan::getUpdateTime, now));

        LifePlan adjusted = new LifePlan();
        adjusted.setUserId(current.getUserId());
        adjusted.setProfileId(current.getProfileId());
        adjusted.setMetricId(current.getMetricId());
        adjusted.setAssessmentId(current.getAssessmentId());
        adjusted.setPlanTitle(blankToDefault(current.getPlanTitle(), "自动优化生活方案"));
        adjusted.setPlanGoal(current.getPlanGoal());
        adjusted.setInputSummary(toJson(Map.of(
                "source", "intervention_review",
                "source_plan_id", current.getPlanId(),
                "review_id", review.getReviewId(),
                "reason", review.getExplanation())));
        adjusted.setPlanJson(toJson(mergedPlan));
        Object checkinTasks = mergedPlan.getOrDefault("checkin_tasks", parseAny(current.getCheckinTasksJson()));
        adjusted.setCheckinTasksJson(toJson(checkinTasks));
        adjusted.setSummary(buildAdjustedPlanSummary(current, review));
        adjusted.setStatus(STATUS_ACTIVE);
        adjusted.setCallStatus(CALL_SUCCESS);
        adjusted.setErrorMessage(null);
        adjusted.setSourceType("auto_intervention");
        adjusted.setSourceReviewId(review.getReviewId());
        adjusted.setCreateTime(now);
        adjusted.setUpdateTime(now);
        lifePlanMapper.insert(adjusted);
        lifePlanMapper.update(null, new LambdaUpdateWrapper<LifePlan>()
                .eq(LifePlan::getUserId, adjusted.getUserId())
                .eq(LifePlan::getStatus, STATUS_ACTIVE)
                .ne(LifePlan::getPlanId, adjusted.getPlanId())
                .set(LifePlan::getStatus, STATUS_HISTORY)
                .set(LifePlan::getUpdateTime, now));
        return adjusted.getPlanId();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergePlanPatch(Map<String, Object> currentPlan, Object patchValue) {
        Map<String, Object> patch = parseObject(patchValue);
        if (patch.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> merged = new LinkedHashMap<>(currentPlan);
        Object patchSchedule = patch.get("daily_schedule");
        if (patchSchedule instanceof List<?> patchDays && !patchDays.isEmpty()) {
            List<Object> currentDays = normalizeScheduleList(merged.get("daily_schedule"));
            Map<Integer, Object> patchByDay = new LinkedHashMap<>();
            for (Object item : patchDays) {
                Map<String, Object> itemMap = parseObject(item);
                Integer day = toInteger(itemMap.get("day"));
                if (day != null) {
                    patchByDay.put(day, itemMap);
                }
            }
            List<Object> adjustedDays = new ArrayList<>();
            for (Object dayValue : currentDays) {
                Map<String, Object> dayMap = parseObject(dayValue);
                Integer day = toInteger(dayMap.get("day"));
                if (day != null && patchByDay.containsKey(day)) {
                    Map<String, Object> patched = new LinkedHashMap<>(dayMap);
                    patched.putAll(parseObject(patchByDay.get(day)));
                    adjustedDays.add(patched);
                } else {
                    adjustedDays.add(dayValue);
                }
            }
            merged.put("daily_schedule", adjustedDays);
            refreshTopLevelPlanSections(merged, adjustedDays);
        }
        for (String key : List.of("health_tips", "summary", "checkin_tasks")) {
            if (patch.containsKey(key)) {
                merged.put(key, patch.get(key));
            }
        }
        return merged;
    }

    private void refreshTopLevelPlanSections(Map<String, Object> merged, List<Object> adjustedDays) {
        List<Map<String, Object>> dayMaps = adjustedDays.stream()
                .map(this::parseObject)
                .filter(item -> !item.isEmpty())
                .toList();
        if (dayMaps.isEmpty()) {
            return;
        }
        Map<String, Object> dietPlan = firstPlanSection(dayMaps, "diet_plan", "dietPlan", "meals", "meal_plan", "diet");
        if (!dietPlan.isEmpty()) {
            merged.put("diet_plan", dietPlan);
        }
        Map<String, Object> exercisePlan = firstPlanSection(dayMaps, "exercise_plan", "exercisePlan", "exercise", "sport", "training");
        if (!exercisePlan.isEmpty()) {
            merged.put("exercise_plan", exercisePlan);
        }
    }

    private Map<String, Object> firstPlanSection(List<Map<String, Object>> dayMaps, String... keys) {
        for (Map<String, Object> day : dayMaps) {
            Object value = firstPresent(day, keys);
            Map<String, Object> section = parseObject(value);
            if (!section.isEmpty()) {
                return section;
            }
        }
        return Map.of();
    }

    private List<Object> normalizeScheduleList(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        if (value instanceof Map<?, ?> map) {
            Object nested = firstPresent(map, "daily_schedule", "weekly_schedule", "days", "plan_days");
            if (nested instanceof List<?> list) {
                return new ArrayList<>(list);
            }
        }
        return List.of();
    }

    private String buildAdjustedPlanSummary(LifePlan current, InterventionReview review) {
        String base = blankToDefault(current.getSummary(), "当前生活方案已根据近期执行情况自动优化。");
        String notice = blankToDefault(review.getPatientNotice(), "系统已根据近期执行情况自动优化后续计划。");
        String explanation = blankToDefault(review.getExplanation(), "");
        return explanation.isEmpty() ? notice + " " + base : notice + " " + explanation;
    }

    private void saveFailedReview(Integer userId, String triggerType, String triggerReason, Exception exception) {
        try {
            LifePlanDTO plan = lifePlanQueryApi.getCurrentPlanByUserId(userId);
            if (plan == null || hasReviewToday(userId, null, triggerType + "_failed")) {
                return;
            }
            InterventionReview review = buildReview(userId, plan, triggerType + "_failed", triggerReason,
                    plan.getCreateTime() == null ? null : buildPlanProgress(plan),
                    LocalDate.now().minusDays(REVIEW_DAYS - 1L), LocalDate.now(),
                    Map.of("trigger_type", triggerType), null, exception);
            interventionReviewMapper.insert(review);
        } catch (Exception ignored) {
            log.warn("Failed to store intervention review failure record");
        }
    }

    private InterventionReviewResponse toResponse(InterventionReview review, boolean includePatch) {
        InterventionReviewResponse response = new InterventionReviewResponse();
        response.setReviewId(review.getReviewId());
        response.setUserId(review.getUserId());
        response.setPlanId(review.getPlanId());
        response.setGeneratedPlanId(review.getGeneratedPlanId());
        response.setTriggerType(review.getTriggerType());
        response.setTriggerReason(review.getTriggerReason());
        response.setPeriodStartDate(review.getPeriodStartDate());
        response.setPeriodEndDate(review.getPeriodEndDate());
        response.setReviewDays(review.getReviewDays());
        response.setPlanDay(review.getPlanDay());
        response.setAdherenceScore(review.getAdherenceScore());
        response.setInterventionLevel(review.getInterventionLevel());
        response.setShouldUpdatePlan(review.getShouldUpdatePlan());
        response.setUpdateScope(review.getUpdateScope());
        response.setAffectedDays(parseIntegerList(review.getAffectedDays()));
        response.setMainProblemTags(parseStringList(review.getMainProblemTags()));
        response.setPreservedItems(parseStringList(review.getPreservedItems()));
        response.setChangedItems(parseStringList(review.getChangedItems()));
        response.setAdjustmentStrategy(review.getAdjustmentStrategy());
        response.setPatientNotice(review.getPatientNotice());
        response.setExplanation(review.getExplanation());
        response.setSafetyWarning(review.getSafetyWarning());
        response.setAdjustedPlanPatch(includePatch ? parseAny(review.getAdjustedPlanPatch()) : null);
        response.setCallStatus(review.getCallStatus());
        response.setErrorMessage(review.getErrorMessage());
        response.setCreateTime(review.getCreateTime());
        return response;
    }

    private List<String> parseStringList(String json) {
        List<?> raw = parseList(json);
        return raw.stream().map(String::valueOf).toList();
    }

    private List<Integer> parseIntegerList(String json) {
        List<?> raw = parseList(json);
        return raw.stream().map(this::toInteger).filter(item -> item != null).toList();
    }

    private List<?> parseList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<?>>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }

    private Object parseAny(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private Map<String, Object> parseObject(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                JsonNode node = objectMapper.readTree(extractJsonText(text));
                return objectMapper.convertValue(node, new TypeReference<>() {});
            } catch (Exception exception) {
                return Map.of();
            }
        }
        return Map.of();
    }

    private String extractJsonText(String text) {
        String trimmed = text.trim();
        try {
            objectMapper.readTree(trimmed);
            return trimmed;
        } catch (Exception ignored) {
            // The response may be natural language wrapped around JSON.
        }
        List<String> candidates = new ArrayList<>();
        int start = trimmed.indexOf('{');
        while (start >= 0 && start < trimmed.length()) {
            int end = findMatchingBrace(trimmed, start);
            if (end > start) {
                candidates.add(trimmed.substring(start, end + 1));
            }
            start = trimmed.indexOf('{', start + 1);
        }
        for (String candidate : candidates) {
            try {
                objectMapper.readTree(candidate);
                return candidate;
            } catch (Exception ignored) {
                // Try the next object if the current candidate is not valid JSON.
            }
        }
        return trimmed;
    }

    private int findMatchingBrace(String text, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private List<?> normalizeList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        if (value == null) {
            return List.of();
        }
        return List.of(value);
    }

    private Object firstPresent(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String blankToDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return false;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
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

    private record PlanProgress(LocalDate startDate, int planDay, int totalDays, boolean expired) {
    }

    private record SafetyAssessment(boolean blockLowerLimbExercise, String reason, List<String> matchedKeywords) {
        private static SafetyAssessment empty() {
            return new SafetyAssessment(false, "", List.of());
        }

        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("block_lower_limb_exercise", blockLowerLimbExercise);
            map.put("reason", reason);
            map.put("matched_keywords", matchedKeywords == null ? List.of() : matchedKeywords);
            map.put("required_action", blockLowerLimbExercise
                    ? "Pause lower-limb exercise and replace future exercise tasks with injury-safe recovery reminders."
                    : "No exercise safety override.");
            return map;
        }
    }

    private record CheckinStats(int totalDays, LocalDate startDate, LocalDate endDate, int dietCompletionCount,
                                int exerciseCompletionCount, int totalTaskCount, int completedTaskCount,
                                BigDecimal completionRate) {
        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("total_days", totalDays);
            map.put("start_date", startDate.toString());
            map.put("end_date", endDate.toString());
            map.put("diet_completion_count", dietCompletionCount);
            map.put("exercise_completion_count", exerciseCompletionCount);
            map.put("total_task_count", totalTaskCount);
            map.put("completed_task_count", completedTaskCount);
            map.put("completion_rate", completionRate);
            return map;
        }
    }
}
