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
            Map<String, Object> fallbackPlan = buildFallbackPlan(profile, metric, assessment, planGoal, planDays, exception.getMessage());
            return transactionTemplate.execute(status -> saveSuccessfulPlan(
                    userId,
                    profile,
                    metric,
                    assessment,
                    planGoal,
                    inputSummary,
                    fallbackPlan,
                    fallbackPlan.getOrDefault("checkin_tasks", List.of()),
                    "AI 服务暂时不可用，系统已根据当前健康档案生成本地可执行方案。"
            ));
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

    private Map<String, Object> buildFallbackPlan(PatientProfileSnapshot profile, HealthMetricSnapshot metric,
                                                  RiskAssessmentSnapshot assessment, String planGoal,
                                                  int planDays, String reason) {
        Map<String, Object> plan = new LinkedHashMap<>();
        String riskLevel = assessment == null ? "中风险" : asString(assessment.getRiskLevel());
        plan.put("plan_title", "个性化控糖生活方案");
        plan.put("plan_goal", planGoal);
        plan.put("risk_level", StringUtils.hasText(riskLevel) ? riskLevel : "中风险");
        plan.put("summary", buildFallbackSummary(profile, metric, riskLevel));
        plan.put("diet_plan", Map.of(
                "principle", "按餐盘法搭配蔬菜、适量主食和优质蛋白，减少含糖饮料和高油菜品。",
                "staple", "主食定量，优先选择全谷物、杂豆、燕麦或杂粮饭。",
                "snack", "两餐之间如需加餐，选择无糖酸奶、少量坚果或小份低糖水果。"
        ));
        plan.put("exercise_plan", Map.of(
                "principle", "以餐后轻运动、规律有氧和轻抗阻训练为主，循序渐进。",
                "notice", "运动前后注意补水和足部保护，如出现明显不适应停止并就医。"
        ));
        plan.put("work_rest_plan", "保持规律睡眠，减少久坐，记录空腹和餐后血糖变化。");
        plan.put("daily_schedule", buildFallbackSchedule(planDays));
        plan.put("health_tips", List.of(
                "本方案用于日常健康管理参考，不能替代线下诊疗。",
                "如血糖明显异常、胸闷头晕或身体不适，请及时咨询线下医生。",
                StringUtils.hasText(reason) ? "AI 服务暂时不可用，已生成本地结构化方案供当前使用。" : "请结合个人感受灵活调整执行强度。"
        ));
        plan.put("checkin_tasks", buildFallbackCheckinTasks());
        plan.put("medical_warning", "如血糖明显异常、身体不适或需要调整用药，请及时咨询线下医生。");
        return plan;
    }

    private String buildFallbackSummary(PatientProfileSnapshot profile, HealthMetricSnapshot metric, String riskLevel) {
        String risk = StringUtils.hasText(riskLevel) ? riskLevel : "中风险";
        String glucose = metric == null || metric.getFastingGlucose() == null ? "近期血糖指标" : "空腹血糖 " + metric.getFastingGlucose() + " mmol/L";
        String family = profile != null && StringUtils.hasText(profile.getFamilyHistory()) ? "、有家族史" : "";
        return "本方案结合" + glucose + family + "和风险评估结果，建议通过饮食结构调整、餐后轻运动、规律作息和每日记录来辅助控糖。方案为期7天，每天包含饮食、运动和提醒任务，请结合自身感受灵活执行。";
    }

    private List<Map<String, Object>> buildFallbackSchedule(int planDays) {
        int days = Math.max(1, Math.min(planDays, 7));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 1; i <= days; i++) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("day", i);
            day.put("title", "第 " + i + " 天");
            day.put("meals", Map.of(
                    "breakfast", i % 2 == 0 ? "燕麦或全麦面包搭配鸡蛋、牛奶或豆制品，避免甜饮料。" : "杂粮粥搭配鸡蛋和少量蔬菜，主食保持定量。",
                    "lunch", "半盘非淀粉蔬菜，搭配适量全谷物主食和鱼禽蛋豆等优质蛋白。",
                    "dinner", "晚餐清淡少油，增加绿叶菜和豆制品，主食比午餐略少。",
                    "snack", "两餐之间如饥饿，可选择无糖酸奶、少量坚果或小份低糖水果。"
            ));
            day.put("exercise_plan", Map.of(
                    "light", "餐后休息片刻后散步 10-20 分钟，保持能轻松说话的强度。",
                    "aerobic", "选择快走、骑车或轻慢跑等有氧活动 20-30 分钟。",
                    "resistance", "用弹力带、靠墙俯卧撑或坐站练习做轻抗阻训练 10-15 分钟。",
                    "notice", "运动前后注意补水和鞋袜舒适，如出现明显不适应立即停止。"
            ));
            day.put("reminder", "记录空腹和餐后血糖，减少久坐，晚间尽量固定时间入睡。");
            result.add(day);
        }
        return result;
    }

    private List<Map<String, Object>> buildFallbackCheckinTasks() {
        return List.of(
                Map.of("task_type", "diet", "task_name", "完成今日饮食建议", "time", "全天"),
                Map.of("task_type", "exercise", "task_name", "完成餐后轻运动", "time", "餐后"),
                Map.of("task_type", "reminder", "task_name", "记录血糖和身体感受", "time", "晚间")
        );
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
