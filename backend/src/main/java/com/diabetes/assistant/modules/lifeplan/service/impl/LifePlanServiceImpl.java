package com.diabetes.assistant.modules.lifeplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.constants.RoleConstants;
import com.diabetes.assistant.common.constants.StatusConstants;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.DateTimeUtil;
import com.diabetes.assistant.modules.dify.dto.DifyWorkflowResult;
import com.diabetes.assistant.modules.dify.service.DifyService;
import com.diabetes.assistant.modules.lifeplan.contract.LifePlanQueryApi;
import com.diabetes.assistant.modules.lifeplan.contract.dto.LifePlanDTO;
import com.diabetes.assistant.modules.lifeplan.dto.GenerateLifePlanRequest;
import com.diabetes.assistant.modules.lifeplan.entity.HealthMetricSnapshot;
import com.diabetes.assistant.modules.lifeplan.entity.LifePlan;
import com.diabetes.assistant.modules.lifeplan.entity.PatientProfileSnapshot;
import com.diabetes.assistant.modules.lifeplan.entity.RiskAssessmentSnapshot;
import com.diabetes.assistant.modules.lifeplan.mapper.HealthMetricSnapshotMapper;
import com.diabetes.assistant.modules.lifeplan.mapper.LifePlanMapper;
import com.diabetes.assistant.modules.lifeplan.mapper.PatientProfileSnapshotMapper;
import com.diabetes.assistant.modules.lifeplan.mapper.RiskAssessmentSnapshotMapper;
import com.diabetes.assistant.modules.lifeplan.service.LifePlanService;
import com.diabetes.assistant.modules.lifeplan.vo.LifePlanResponse;
import com.diabetes.assistant.modules.lifeplan.vo.MissingLifePlanDataResponse;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import com.diabetes.assistant.modules.user.contract.dto.UserBasicDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LifePlanServiceImpl implements LifePlanService, LifePlanQueryApi {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_HISTORY = "history";
    private static final String CALL_SUCCESS = "success";
    private static final String CALL_FAILED = "failed";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final LifePlanMapper lifePlanMapper;
    private final PatientProfileSnapshotMapper profileMapper;
    private final HealthMetricSnapshotMapper metricMapper;
    private final RiskAssessmentSnapshotMapper assessmentMapper;
    private final DifyService difyService;
    private final UserQueryApi userQueryApi;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    public String entry() {
        return "生活方案模块功能开发中";
    }

    @Override
    public LifePlanResponse generate(Integer userId, GenerateLifePlanRequest request) {
        requirePatient(userId);
        GenerateLifePlanRequest safeRequest = request == null ? new GenerateLifePlanRequest() : request;
        String planGoal = required(safeRequest.getPlanGoal(), "plan_goal不能为空");
        int planDays = validPlanDays(safeRequest.getPlanDays());
        List<String> avoidItems = safeAvoidItems(safeRequest.getAvoidItems());

        PatientProfileSnapshot profile = findProfile(userId);
        HealthMetricSnapshot metric = findLatestMetric(userId);
        RiskAssessmentSnapshot assessment = findLatestSuccessfulAssessment(userId);
        List<String> missing = missingData(profile, metric, assessment);
        if (!missing.isEmpty()) {
            throw new LifePlanMissingDataException(new MissingLifePlanDataResponse(missing));
        }

        if (metric.getWeightKg() == null && profile.getBaseWeightKg() != null) {
            metric.setWeightKg(profile.getBaseWeightKg());
        }

        Map<String, Object> inputSummary = buildInputSummary(profile, metric, assessment, planGoal, avoidItems, planDays);
        DifyWorkflowResult result;
        try {
            result = difyService.callLifePlan(buildDifyInputs(userId, inputSummary), "user-" + userId);
        } catch (BusinessException exception) {
            saveFailedPlanInTransaction(userId, profile, metric, assessment, planGoal, inputSummary, exception.getMessage());
            throw exception;
        }

        return handleDifyOutputs(userId, profile, metric, assessment, planGoal, inputSummary, result.getOutputs());
    }

    @Override
    public PageResult<LifePlanResponse> listUserPlans(Integer userId, Integer page, Integer pageSize, String status, String callStatus) {
        requireLoggedIn(userId);
        validateStatus(status);
        validateCallStatus(callStatus);
        int currentPage = validPage(page);
        int currentPageSize = validPageSize(pageSize);

        Page<LifePlan> result = lifePlanMapper.selectPage(Page.of(currentPage, currentPageSize), new LambdaQueryWrapper<LifePlan>()
                .eq(LifePlan::getUserId, userId)
                .eq(StringUtils.hasText(status), LifePlan::getStatus, normalize(status))
                .eq(StringUtils.hasText(callStatus), LifePlan::getCallStatus, normalize(callStatus))
                .orderByDesc(LifePlan::getCreateTime)
                .orderByDesc(LifePlan::getPlanId));

        List<LifePlanResponse> list = result.getRecords().stream()
                .map(plan -> toResponse(plan, false, false))
                .toList();
        return new PageResult<>(list, result.getTotal(), currentPage, currentPageSize);
    }

    @Override
    public LifePlanResponse getUserPlanDetail(Integer userId, Integer planId) {
        requireLoggedIn(userId);
        LifePlan plan = lifePlanMapper.selectOne(new LambdaQueryWrapper<LifePlan>()
                .eq(LifePlan::getPlanId, planId)
                .eq(LifePlan::getUserId, userId)
                .last("LIMIT 1"));
        if (plan == null) {
            throw new BusinessException(404, "生活方案不存在");
        }
        return toResponse(plan, true, false);
    }

    @Override
    public PageResult<LifePlanResponse> listAdminPlans(Integer adminUserId, Integer page, Integer pageSize, Integer planId, Integer userId,
                                                       String keyword, String status, String callStatus,
                                                       LocalDate startDate, LocalDate endDate) {
        requireAdmin(adminUserId);
        String normalizedStatus = normalize(status);
        String normalizedCallStatus = normalize(callStatus);
        if (CALL_FAILED.equals(normalizedStatus) && !StringUtils.hasText(normalizedCallStatus)) {
            normalizedCallStatus = CALL_FAILED;
            normalizedStatus = null;
        }
        validateStatus(normalizedStatus);
        validateCallStatus(normalizedCallStatus);
        int currentPage = validPage(page);
        int currentPageSize = validPageSize(pageSize);

        List<Integer> keywordUserIds = findUserIdsByKeyword(keyword);
        Integer keywordPlanId = parseInteger(normalize(keyword));
        Page<LifePlan> result = lifePlanMapper.selectPage(Page.of(currentPage, currentPageSize), new LambdaQueryWrapper<LifePlan>()
                .eq(planId != null, LifePlan::getPlanId, planId)
                .eq(userId != null, LifePlan::getUserId, userId)
                .eq(StringUtils.hasText(normalizedStatus), LifePlan::getStatus, normalizedStatus)
                .eq(StringUtils.hasText(normalizedCallStatus), LifePlan::getCallStatus, normalizedCallStatus)
                .ge(startDate != null, LifePlan::getCreateTime, startDate == null ? null : startDate.atStartOfDay())
                .lt(endDate != null, LifePlan::getCreateTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .and(StringUtils.hasText(keyword), nested -> {
                    if (!keywordUserIds.isEmpty()) {
                        nested.in(LifePlan::getUserId, keywordUserIds);
                    }
                    if (keywordPlanId != null) {
                        if (!keywordUserIds.isEmpty()) {
                            nested.or();
                        }
                        nested.eq(LifePlan::getPlanId, keywordPlanId);
                    }
                    if (keywordUserIds.isEmpty() && keywordPlanId == null) {
                        nested.eq(LifePlan::getPlanId, -1);
                    }
                })
                .orderByDesc(LifePlan::getCreateTime)
                .orderByDesc(LifePlan::getPlanId));

        List<LifePlanResponse> list = result.getRecords().stream()
                .map(plan -> toResponse(plan, true, true))
                .toList();
        return new PageResult<>(list, result.getTotal(), currentPage, currentPageSize);
    }

    @Override
    public LifePlanDTO getCurrentPlanByUserId(Integer userId) {
        LifePlan plan = getCurrentPlan(userId);
        return plan == null ? null : toDto(plan);
    }

    @Override
    public Integer getCurrentPlanIdByUserId(Integer userId) {
        LifePlan plan = getCurrentPlan(userId);
        return plan == null ? null : plan.getPlanId();
    }

    @Override
    public String getCurrentLifePlanSummaryByUserId(Integer userId) {
        LifePlan plan = getCurrentPlan(userId);
        return plan == null ? null : plan.getSummary();
    }

    @Override
    public String getCurrentCheckinTasksJsonByUserId(Integer userId) {
        LifePlan plan = getCurrentPlan(userId);
        return plan == null ? null : plan.getCheckinTasksJson();
    }

    private LifePlanResponse saveSuccessfulPlan(Integer userId, PatientProfileSnapshot profile, HealthMetricSnapshot metric,
                                                RiskAssessmentSnapshot assessment, String planGoal,
                                                Map<String, Object> inputSummary, Map<String, Object> planResult,
                                                Object checkinTasks, String outputInputSummary) {
        LocalDateTime now = LocalDateTime.now();
        lifePlanMapper.update(null, new LambdaUpdateWrapper<LifePlan>()
                .eq(LifePlan::getUserId, userId)
                .eq(LifePlan::getStatus, STATUS_ACTIVE)
                .set(LifePlan::getStatus, STATUS_HISTORY)
                .set(LifePlan::getUpdateTime, now));

        LifePlan plan = basePlan(userId, profile, metric, assessment, planGoal, inputSummary);
        plan.setPlanTitle(asString(planResult.getOrDefault("plan_title", "个性化控糖生活方案")));
        plan.setSummary(asString(planResult.getOrDefault("summary", "已生成个性化生活方案")));
        plan.setPlanJson(toJson(planResult));
        plan.setCheckinTasksJson(toJson(checkinTasks));
        plan.setInputSummary(toJson(mergeInputSummary(inputSummary, outputInputSummary)));
        plan.setStatus(STATUS_ACTIVE);
        plan.setCallStatus(CALL_SUCCESS);
        plan.setCreateTime(now);
        plan.setUpdateTime(now);
        lifePlanMapper.insert(plan);
        return toResponse(plan, true, false);
    }

    private LifePlanResponse handleDifyOutputs(Integer userId, PatientProfileSnapshot profile, HealthMetricSnapshot metric,
                                               RiskAssessmentSnapshot assessment, String planGoal,
                                               Map<String, Object> inputSummary, Map<String, Object> outputs) {
        boolean success = Boolean.TRUE.equals(outputs.get("success")) || "true".equalsIgnoreCase(asString(outputs.get("success")));
        String errorMessage = asString(outputs.get("error_message"));
        Object rawPlanResult = outputs.get("plan_result");
        if (rawPlanResult == null && looksLikeSplitOutput(outputs)) {
            rawPlanResult = buildPlanResultFromSplitOutputs(outputs);
        }

        Map<String, Object> planResult = parseObject(rawPlanResult);
        if (!success) {
            saveFailedPlanInTransaction(userId, profile, metric, assessment, planGoal, inputSummary,
                    StringUtils.hasText(errorMessage) ? errorMessage : "Dify 工作流返回失败");
            throw new BusinessException(502, StringUtils.hasText(errorMessage) ? errorMessage : "Dify 工作流返回失败");
        }
        if (planResult.isEmpty()) {
            saveFailedPlanInTransaction(userId, profile, metric, assessment, planGoal, inputSummary, "Dify 返回格式异常：plan_result为空");
            throw new BusinessException(502, "Dify 返回格式异常：plan_result为空");
        }

        Object checkinTasks = planResult.getOrDefault("checkin_tasks", List.of());
        return transactionTemplate.execute(status -> saveSuccessfulPlan(userId, profile, metric, assessment, planGoal, inputSummary, planResult, checkinTasks, asString(outputs.get("input_summary"))));
    }

    private void saveFailedPlanInTransaction(Integer userId, PatientProfileSnapshot profile, HealthMetricSnapshot metric,
                                             RiskAssessmentSnapshot assessment, String planGoal,
                                             Map<String, Object> inputSummary, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> saveFailedPlan(userId, profile, metric, assessment, planGoal, inputSummary, errorMessage));
    }

    private void saveFailedPlan(Integer userId, PatientProfileSnapshot profile, HealthMetricSnapshot metric,
                                RiskAssessmentSnapshot assessment, String planGoal,
                                Map<String, Object> inputSummary, String errorMessage) {
        LifePlan plan = basePlan(userId, profile, metric, assessment, planGoal, inputSummary);
        plan.setPlanTitle("个性化控糖生活方案");
        plan.setPlanJson(toJson(Map.of()));
        plan.setCheckinTasksJson(toJson(List.of()));
        plan.setSummary(null);
        plan.setStatus(STATUS_HISTORY);
        plan.setCallStatus(CALL_FAILED);
        plan.setErrorMessage(errorMessage);
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        lifePlanMapper.insert(plan);
    }

    private LifePlan basePlan(Integer userId, PatientProfileSnapshot profile, HealthMetricSnapshot metric,
                              RiskAssessmentSnapshot assessment, String planGoal, Map<String, Object> inputSummary) {
        LifePlan plan = new LifePlan();
        plan.setUserId(userId);
        plan.setProfileId(profile == null ? null : profile.getProfileId());
        plan.setMetricId(metric == null ? null : metric.getMetricId());
        plan.setAssessmentId(assessment == null ? null : assessment.getAssessmentId());
        plan.setPlanGoal(planGoal);
        plan.setInputSummary(toJson(inputSummary));
        return plan;
    }

    private PatientProfileSnapshot findProfile(Integer userId) {
        return profileMapper.selectOne(new LambdaQueryWrapper<PatientProfileSnapshot>()
                .eq(PatientProfileSnapshot::getUserId, userId)
                .orderByDesc(PatientProfileSnapshot::getUpdateTime)
                .orderByDesc(PatientProfileSnapshot::getProfileId)
                .last("LIMIT 1"));
    }

    private HealthMetricSnapshot findLatestMetric(Integer userId) {
        return metricMapper.selectOne(new LambdaQueryWrapper<HealthMetricSnapshot>()
                .eq(HealthMetricSnapshot::getUserId, userId)
                .orderByDesc(HealthMetricSnapshot::getRecordedAt)
                .orderByDesc(HealthMetricSnapshot::getMetricId)
                .last("LIMIT 1"));
    }

    private RiskAssessmentSnapshot findLatestSuccessfulAssessment(Integer userId) {
        return assessmentMapper.selectOne(new LambdaQueryWrapper<RiskAssessmentSnapshot>()
                .eq(RiskAssessmentSnapshot::getUserId, userId)
                .eq(RiskAssessmentSnapshot::getCallStatus, CALL_SUCCESS)
                .orderByDesc(RiskAssessmentSnapshot::getCreateTime)
                .orderByDesc(RiskAssessmentSnapshot::getAssessmentId)
                .last("LIMIT 1"));
    }

    private List<String> missingData(PatientProfileSnapshot profile, HealthMetricSnapshot metric, RiskAssessmentSnapshot assessment) {
        List<String> missing = new ArrayList<>();
        if (profile == null) {
            missing.add("patient_profile");
        }
        if (metric == null) {
            missing.add("latest_health_metric");
        }
        if (assessment == null) {
            missing.add("latest_risk_assessment");
        }
        return missing;
    }

    private Map<String, Object> buildInputSummary(PatientProfileSnapshot profile, HealthMetricSnapshot metric,
                                                  RiskAssessmentSnapshot assessment, String planGoal,
                                                  List<String> avoidItems, int planDays) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("profile_id", profile.getProfileId());
        input.put("metric_id", metric.getMetricId());
        input.put("assessment_id", assessment.getAssessmentId());
        input.put("plan_goal", planGoal);
        input.put("avoid_items", avoidItems);
        input.put("plan_days", planDays);
        input.put("user_profile_json", toProfileMap(profile));
        input.put("latest_health_data_json", toMetricMap(metric));
        input.put("risk_result_json", toRiskMap(assessment));
        return input;
    }

    private Map<String, Object> buildDifyInputs(Integer userId, Map<String, Object> inputSummary) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("user_id", String.valueOf(userId));
        inputs.put("user_profile_json", toJson(inputSummary.get("user_profile_json")));
        inputs.put("latest_health_data_json", toJson(inputSummary.get("latest_health_data_json")));
        inputs.put("risk_result_json", toJson(inputSummary.get("risk_result_json")));
        inputs.put("plan_goal", inputSummary.get("plan_goal"));
        inputs.put("avoid_items", toJson(inputSummary.get("avoid_items")));
        inputs.put("plan_days", String.valueOf(inputSummary.get("plan_days")));
        return inputs;
    }

    private Map<String, Object> toProfileMap(PatientProfileSnapshot profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("age", profile.getAge());
        map.put("gender", profile.getGender());
        map.put("height_cm", profile.getHeightCm());
        map.put("base_weight_kg", profile.getBaseWeightKg());
        map.put("base_waist_cm", profile.getBaseWaistCm());
        map.put("family_history", profile.getFamilyHistory());
        map.put("chronic_history", profile.getChronicHistory());
        map.put("allergy_history", profile.getAllergyHistory());
        return map;
    }

    private Map<String, Object> toMetricMap(HealthMetricSnapshot metric) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("weight_kg", metric.getWeightKg());
        map.put("waist_cm", metric.getWaistCm());
        map.put("systolic_bp", metric.getSystolicBp());
        map.put("diastolic_bp", metric.getDiastolicBp());
        map.put("fasting_glucose", metric.getFastingGlucose());
        map.put("postprandial_glucose", metric.getPostprandialGlucose());
        map.put("hba1c", metric.getHba1c());
        map.put("diet_status", metric.getDietStatus());
        map.put("exercise_status", metric.getExerciseStatus());
        map.put("recorded_at", metric.getRecordedAt() == null ? null : metric.getRecordedAt().toString());
        return map;
    }

    private Map<String, Object> toRiskMap(RiskAssessmentSnapshot assessment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("risk_level", assessment.getRiskLevel());
        map.put("risk_score", assessment.getRiskScore());
        map.put("diabetes_type_tendency", assessment.getDiabetesTypeTendency());
        map.put("main_risk_factors", parseAny(assessment.getMainRiskFactors()));
        map.put("indicator_analysis", assessment.getIndicatorAnalysis());
        map.put("health_advice", assessment.getHealthAdvice());
        map.put("medical_warning", assessment.getMedicalWarning());
        map.put("summary", assessment.getSummary());
        return map;
    }

    private Map<String, Object> buildPlanResultFromSplitOutputs(Map<String, Object> outputs) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("plan_title", outputs.getOrDefault("planTitle", outputs.get("plan_title")));
        plan.put("plan_goal", outputs.getOrDefault("planGoal", outputs.get("plan_goal")));
        plan.put("diet_plan", parseAny(outputs.getOrDefault("dietPlanJson", outputs.get("diet_plan"))));
        plan.put("exercise_plan", parseAny(outputs.getOrDefault("exercisePlanJson", outputs.get("exercise_plan"))));
        plan.put("daily_schedule", parseAny(outputs.getOrDefault("dailyScheduleJson", outputs.get("daily_schedule"))));
        plan.put("checkin_tasks", parseAny(outputs.getOrDefault("checkinTasksJson", outputs.get("checkin_tasks"))));
        plan.put("health_tips", parseAny(outputs.getOrDefault("healthTipsJson", outputs.get("health_tips"))));
        plan.put("summary", outputs.get("summary"));
        return plan;
    }

    private boolean looksLikeSplitOutput(Map<String, Object> outputs) {
        return outputs.containsKey("dietPlanJson") || outputs.containsKey("diet_plan") || outputs.containsKey("planTitle");
    }

    private Map<String, Object> mergeInputSummary(Map<String, Object> inputSummary, String outputInputSummary) {
        Map<String, Object> merged = new LinkedHashMap<>(inputSummary);
        if (StringUtils.hasText(outputInputSummary)) {
            merged.put("dify_input_summary", outputInputSummary);
        }
        return merged;
    }

    private LifePlanResponse toResponse(LifePlan plan, boolean includeJson, boolean includeUser) {
        UserBasicDTO user = includeUser ? userQueryApi.getUserBasicById(plan.getUserId()) : null;
        RiskAssessmentSnapshot risk = plan.getAssessmentId() == null ? null : assessmentMapper.selectById(plan.getAssessmentId());
        return LifePlanResponse.builder()
                .planId(plan.getPlanId())
                .userId(plan.getUserId())
                .username(user == null ? null : user.getUsername())
                .phone(user == null ? null : user.getPhone())
                .profileId(plan.getProfileId())
                .metricId(plan.getMetricId())
                .assessmentId(plan.getAssessmentId())
                .planTitle(plan.getPlanTitle())
                .planGoal(plan.getPlanGoal())
                .inputSummary(includeJson ? plan.getInputSummary() : null)
                .planJson(includeJson ? parseAny(plan.getPlanJson()) : null)
                .checkinTasksJson(includeJson ? parseAny(plan.getCheckinTasksJson()) : null)
                .summary(plan.getSummary())
                .status(plan.getStatus())
                .callStatus(plan.getCallStatus())
                .errorMessage(includeJson ? plan.getErrorMessage() : null)
                .riskLevel(risk == null ? null : risk.getRiskLevel())
                .riskScore(risk == null ? null : risk.getRiskScore())
                .createTime(DateTimeUtil.format(plan.getCreateTime()))
                .updateTime(DateTimeUtil.format(plan.getUpdateTime()))
                .build();
    }

    private LifePlanDTO toDto(LifePlan plan) {
        LifePlanDTO dto = new LifePlanDTO();
        dto.setPlanId(plan.getPlanId());
        dto.setUserId(plan.getUserId());
        dto.setAssessmentId(plan.getAssessmentId());
        dto.setPlanTitle(plan.getPlanTitle());
        dto.setPlanGoal(plan.getPlanGoal());
        dto.setDietPlanJson(extractJsonField(plan.getPlanJson(), "diet_plan"));
        dto.setExercisePlanJson(extractJsonField(plan.getPlanJson(), "exercise_plan"));
        dto.setDailyScheduleJson(extractJsonField(plan.getPlanJson(), "daily_schedule"));
        dto.setCheckinTasksJson(plan.getCheckinTasksJson());
        dto.setHealthTipsJson(extractJsonField(plan.getPlanJson(), "health_tips"));
        dto.setSummary(plan.getSummary());
        dto.setStatus(plan.getStatus());
        dto.setCallStatus(plan.getCallStatus());
        dto.setErrorMessage(plan.getErrorMessage());
        dto.setCreateTime(plan.getCreateTime());
        return dto;
    }

    private LifePlan getCurrentPlan(Integer userId) {
        return lifePlanMapper.selectOne(new LambdaQueryWrapper<LifePlan>()
                .eq(LifePlan::getUserId, userId)
                .eq(LifePlan::getStatus, STATUS_ACTIVE)
                .eq(LifePlan::getCallStatus, CALL_SUCCESS)
                .orderByDesc(LifePlan::getCreateTime)
                .last("LIMIT 1"));
    }

    private String extractJsonField(String json, String key) {
        Object value = parseObject(json).get(key);
        return value == null ? null : toJson(value);
    }

    private void requireLoggedIn(Integer userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
    }

    private void requirePatient(Integer userId) {
        requireLoggedIn(userId);
        UserBasicDTO user = userQueryApi.getUserBasicById(userId);
        if (user == null || !StatusConstants.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(401, "请先登录");
        }
        if (!RoleConstants.PATIENT.equals(user.getRole())) {
            throw new BusinessException(403, "只有患者用户可以生成生活方案");
        }
    }

    private void requireAdmin(Integer adminUserId) {
        if (!userQueryApi.isAdmin(adminUserId)) {
            throw new BusinessException(403, "无权限访问管理员接口");
        }
    }

    private List<Integer> findUserIdsByKeyword(String keyword) {
        String normalizedKeyword = normalize(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return List.of();
        }
        return userQueryApi.searchUserIdsByKeyword(normalizedKeyword);
    }

    private void validateStatus(String status) {
        String normalized = normalize(status);
        if (StringUtils.hasText(normalized) && !STATUS_ACTIVE.equals(normalized) && !STATUS_HISTORY.equals(normalized)) {
            throw new BusinessException(400, "status参数不合法");
        }
    }

    private void validateCallStatus(String callStatus) {
        String normalized = normalize(callStatus);
        if (StringUtils.hasText(normalized) && !CALL_SUCCESS.equals(normalized) && !CALL_FAILED.equals(normalized)) {
            throw new BusinessException(400, "call_status参数不合法");
        }
    }

    private String required(String value, String message) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(400, message);
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int validPlanDays(Integer planDays) {
        if (planDays == null) {
            return 7;
        }
        if (planDays < 1 || planDays > 30) {
            throw new BusinessException(400, "plan_days必须在1到30之间");
        }
        return planDays;
    }

    private List<String> safeAvoidItems(List<String> avoidItems) {
        if (avoidItems == null) {
            return List.of();
        }
        return avoidItems.stream()
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private int validPage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int validPageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, 100);
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "JSON序列化失败");
        }
    }

    private Object parseAny(Object value) {
        if (!(value instanceof String stringValue)) {
            return value;
        }
        try {
            return objectMapper.readValue(stringValue, Object.class);
        } catch (JsonProcessingException exception) {
            return stringValue;
        }
    }

    private Map<String, Object> parseObject(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            try {
                return objectMapper.readValue(stringValue, new TypeReference<>() {});
            } catch (JsonProcessingException exception) {
                return Map.of();
            }
        }
        return Map.of();
    }
}
