package com.diabetes.assistant.modules.risk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.PageUtils;
import com.diabetes.assistant.modules.dify.config.DifyProperties;
import com.diabetes.assistant.modules.dify.service.DifyService;
import com.diabetes.assistant.common.utils.GenderUtils;
import com.diabetes.assistant.modules.healthmetric.contract.HealthMetricQueryApi;
import com.diabetes.assistant.modules.healthmetric.contract.dto.HealthMetricDTO;
import com.diabetes.assistant.modules.healthmetric.entity.HealthMetric;
import com.diabetes.assistant.modules.healthmetric.mapper.HealthMetricMapper;
import com.diabetes.assistant.modules.checkin.contract.CheckinQueryApi;
import com.diabetes.assistant.modules.healthmetric.util.MetricAbnormalUtils;
import com.diabetes.assistant.modules.interventionreview.service.InterventionReviewTriggerService;
import com.diabetes.assistant.modules.lifeplan.contract.LifePlanQueryApi;
import com.diabetes.assistant.modules.lifeplan.contract.dto.LifePlanDTO;
import com.diabetes.assistant.modules.profile.contract.PatientProfileQueryApi;
import com.diabetes.assistant.modules.profile.contract.dto.PatientProfileDTO;
import com.diabetes.assistant.modules.profile.entity.PatientProfile;
import com.diabetes.assistant.modules.profile.mapper.PatientProfileMapper;
import com.diabetes.assistant.modules.risk.contract.RiskAssessmentQueryApi;
import com.diabetes.assistant.modules.risk.contract.dto.RiskAssessmentDTO;
import com.diabetes.assistant.modules.risk.dto.AdminRiskListItem;
import com.diabetes.assistant.modules.risk.dto.PatientSimilarCaseItem;
import com.diabetes.assistant.modules.risk.dto.RiskDetailResponse;
import com.diabetes.assistant.modules.risk.dto.RiskEntryResponse;
import com.diabetes.assistant.modules.risk.dto.RiskHistoryItem;
import com.diabetes.assistant.modules.risk.dto.RiskPredictResponse;
import com.diabetes.assistant.modules.risk.dto.RiskTrendResponse;
import com.diabetes.assistant.modules.risk.dto.SimilarCaseItem;
import com.diabetes.assistant.modules.risk.entity.RiskAssessment;
import com.diabetes.assistant.modules.risk.mapper.RiskAssessmentMapper;
import com.diabetes.assistant.modules.risk.service.RiskService;
import com.diabetes.assistant.modules.risk.util.PatientSimilarCaseMapper;
import com.diabetes.assistant.modules.risk.util.RiskResultParser;
import com.diabetes.assistant.modules.risk.util.RiskResultParser.ParsedRiskResult;
import com.diabetes.assistant.modules.risk.util.SimilarCaseReferenceBuilder;
import com.diabetes.assistant.modules.risk.util.RiskSimilarityUtil;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import com.diabetes.assistant.modules.user.contract.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiskServiceImpl implements RiskService, RiskAssessmentQueryApi {

    private final RiskAssessmentMapper riskAssessmentMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final HealthMetricMapper healthMetricMapper;
    private final PatientProfileQueryApi patientProfileQueryApi;
    private final HealthMetricQueryApi healthMetricQueryApi;
    private final DifyService difyService;
    private final DifyProperties difyProperties;
    private final UserQueryApi userQueryApi;
    private final LifePlanQueryApi lifePlanQueryApi;
    private final CheckinQueryApi checkinQueryApi;
    private final InterventionReviewTriggerService interventionReviewTriggerService;

    @Override
    public RiskEntryResponse getEntry(Integer userId) {
        RiskEntryResponse response = new RiskEntryResponse();
        RiskAssessment latest = findLatestByUserId(userId);
        response.setLatestAssessment(latest == null ? null : toDetailResponse(latest));

        List<String> missing = collectMissing(userId);
        response.setCanPredict(missing.isEmpty());
        if (!missing.isEmpty()) {
            response.setMissingReason("缺少：" + String.join("、", missing));
        }
        return response;
    }

    @Override
    public RiskPredictResponse predictRisk(Integer userId) {
        List<String> missing = new ArrayList<>();
        PatientProfileDTO profile = patientProfileQueryApi.getProfileByUserId(userId);
        HealthMetricDTO metric = healthMetricQueryApi.getLatestMetricByUserId(userId);
        if (profile == null) {
            missing.add("patient_profile");
        }
        if (metric == null) {
            missing.add("latest_health_metric");
        }
        if (!missing.isEmpty()) {
            throw new BusinessException(400,
                    "缺少健康档案或最新健康数据，请先填写个人信息并录入健康数据",
                    Map.of("missing", missing));
        }

        Map<String, Object> inputs = buildDifyInputs(profile, metric);
        String requestSummary = buildRequestSummary(profile, metric, inputs);
        String rawResponse = null;
        ParsedRiskResult parsed;
        String callStatus;
        String errorMessage = null;

        try {
            if (shouldUseDevMock()) {
                rawResponse = buildDevMockResponse();
            } else {
                rawResponse = difyService.callRiskPrediction(inputs, String.valueOf(userId));
                if (rawResponse.startsWith("TODO")) {
                    rawResponse = buildDevMockResponse();
                }
            }
            parsed = completeParsedResult(RiskResultParser.parse(rawResponse), profile, metric);
            callStatus = "success";
        } catch (Exception exception) {
            callStatus = "failed";
            errorMessage = buildErrorMessage(exception);
            parsed = null;

            RiskAssessment failed = new RiskAssessment();
            failed.setUserId(userId);
            failed.setMetricId(metric.getMetricId());
            failed.setRequestSummary(requestSummary);
            failed.setResponseResult(rawResponse);
            failed.setCallStatus(callStatus);
            failed.setErrorMessage(errorMessage);
            riskAssessmentMapper.insert(failed);

            throw new BusinessException(502, "AI服务调用失败，请稍后重试",
                    Map.of("error_message", errorMessage == null ? "未知错误" : errorMessage));
        }

        RiskAssessment assessment = new RiskAssessment();
        assessment.setUserId(userId);
        assessment.setMetricId(metric.getMetricId());
        assessment.setRequestSummary(requestSummary);
        assessment.setResponseResult(rawResponse);
        applyParsedResult(assessment, parsed);
        assessment.setCallStatus(callStatus);
        riskAssessmentMapper.insert(assessment);
        interventionReviewTriggerService.triggerAsync(userId, "risk_assessment_save", "Risk assessment result was updated");

        return toPredictResponse(assessment, parsed);
    }

    @Override
    public RiskDetailResponse getLatestAssessment(Integer userId) {
        RiskAssessment assessment = findLatestByUserId(userId);
        return assessment == null ? null : toDetailResponse(assessment);
    }

    @Override
    public PageResult<RiskHistoryItem> getHistory(Integer userId, Integer page, Integer pageSize,
                                                  LocalDate startDate, LocalDate endDate) {
        int currentPage = PageUtils.normalizePage(page);
        int size = PageUtils.normalizePageSize(pageSize);

        LambdaQueryWrapper<RiskAssessment> wrapper = buildUserDateWrapper(userId, startDate, endDate);
        wrapper.orderByDesc(RiskAssessment::getCreateTime);

        Page<RiskAssessment> pageResult = riskAssessmentMapper.selectPage(new Page<>(currentPage, size), wrapper);
        List<RiskHistoryItem> list = pageResult.getRecords().stream()
                .map(this::toHistoryItem)
                .toList();
        return new PageResult<>(list, pageResult.getTotal(), currentPage, size);
    }

    @Override
    public RiskDetailResponse getAssessmentDetail(Integer userId, Integer assessmentId) {
        RiskAssessment assessment = requireOwnedAssessment(assessmentId, userId);
        return toDetailResponse(assessment);
    }

    @Override
    public PageResult<AdminRiskListItem> adminListAssessments(Integer userId, String riskLevel,
                                                              LocalDate startDate, LocalDate endDate,
                                                              Integer page, Integer pageSize) {
        int currentPage = PageUtils.normalizePage(page);
        int size = PageUtils.normalizePageSize(pageSize);

        LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(RiskAssessment::getUserId, userId);
        }
        if (StringUtils.hasText(riskLevel)) {
            wrapper.eq(RiskAssessment::getRiskLevel, riskLevel);
        }
        applyDateRange(wrapper, startDate, endDate);
        wrapper.orderByDesc(RiskAssessment::getCreateTime);

        Page<RiskAssessment> pageResult = riskAssessmentMapper.selectPage(new Page<>(currentPage, size), wrapper);
        List<AdminRiskListItem> list = pageResult.getRecords().stream()
                .map(this::toAdminListItem)
                .toList();
        return new PageResult<>(list, pageResult.getTotal(), currentPage, size);
    }

    @Override
    public RiskDetailResponse adminGetAssessmentDetail(Integer assessmentId) {
        RiskAssessment assessment = riskAssessmentMapper.selectById(assessmentId);
        if (assessment == null) {
            throw new BusinessException(404, "评估记录不存在");
        }
        return toDetailResponse(assessment);
    }

    @Override
    public RiskTrendResponse getRiskTrend(Integer userId) {
        return buildRiskTrend(userId);
    }

    @Override
    public RiskTrendResponse adminGetRiskTrend(Integer userId) {
        if (userId == null) {
            throw new BusinessException(400, "user_id 不能为空");
        }
        return buildRiskTrend(userId);
    }

    @Override
    public List<SimilarCaseItem> adminGetSimilarCases(Integer assessmentId, Integer limit) {
        RiskAssessment assessment = riskAssessmentMapper.selectById(assessmentId);
        if (assessment == null) {
            throw new BusinessException(404, "评估记录不存在");
        }
        int topK = normalizeSimilarCaseLimit(limit, 3, 10);
        return buildSimilarCases(assessment, topK);
    }

    @Override
    public List<PatientSimilarCaseItem> getSimilarCases(Integer userId, Integer assessmentId, Integer limit) {
        RiskAssessment assessment = requireOwnedAssessment(assessmentId, userId);
        int topK = normalizeSimilarCaseLimit(limit, 2, 5);
        List<SimilarCaseItem> items = buildSimilarCases(assessment, topK);
        List<PatientSimilarCaseItem> result = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            result.add(PatientSimilarCaseMapper.toPatientItem(i + 1, items.get(i)));
        }
        return result;
    }

    private List<SimilarCaseItem> buildSimilarCases(RiskAssessment assessment, int topK) {
        PatientProfileDTO sourceProfile = patientProfileQueryApi.getProfileByUserId(assessment.getUserId());
        HealthMetricDTO sourceMetric = resolveMetricForAssessment(assessment);
        if (sourceProfile == null || sourceMetric == null) {
            return List.of();
        }

        List<SimilarCaseItem> candidates = new ArrayList<>();

        for (PatientProfile profile : patientProfileMapper.selectList(null)) {
            if (profile.getUserId().equals(assessment.getUserId())) {
                continue;
            }
            UserBasicDTO user = userQueryApi.getUserBasicById(profile.getUserId());
            if (user == null || !"active".equalsIgnoreCase(user.getStatus()) || !"patient".equalsIgnoreCase(user.getRole())) {
                continue;
            }

            PatientProfileDTO candidateProfile = patientProfileQueryApi.getProfileByUserId(profile.getUserId());
            HealthMetricDTO candidateMetric = healthMetricQueryApi.getLatestMetricByUserId(profile.getUserId());
            if (candidateProfile == null || candidateMetric == null) {
                continue;
            }

            int similarity = RiskSimilarityUtil.calculateSimilarity(
                    sourceProfile, sourceMetric, candidateProfile, candidateMetric);
            if (similarity < 35) {
                continue;
            }

            SimilarCaseItem item = new SimilarCaseItem();
            item.setUserId(profile.getUserId());
            item.setUsername(user.getUsername());
            item.setAge(candidateProfile.getAge());
            item.setGender(GenderUtils.toDisplayLabel(candidateProfile.getGender()));
            item.setSimilarityScore(similarity);
            item.setFastingGlucose(RiskSimilarityUtil.round(candidateMetric.getFastingGlucose()));
            item.setWeightKg(RiskSimilarityUtil.round(candidateMetric.getWeightKg()));
            item.setWaistCm(RiskSimilarityUtil.round(
                    candidateMetric.getWaistCm() != null ? candidateMetric.getWaistCm() : candidateProfile.getBaseWaistCm()));
            item.setMatchReason(RiskSimilarityUtil.buildMatchReason(
                    sourceProfile, sourceMetric, candidateProfile, candidateMetric));

            RiskAssessment latestRisk = findLatestSuccessByUserId(profile.getUserId());
            if (latestRisk != null) {
                item.setRiskLevel(latestRisk.getRiskLevel());
                item.setRiskScore(latestRisk.getRiskScore());
            }

            LifePlanDTO plan = lifePlanQueryApi.getCurrentPlanByUserId(profile.getUserId());
            String planTitle = plan == null ? null : plan.getPlanTitle();
            String planSummary = plan == null ? null : plan.getSummary();
            BigDecimal completionRate = checkinQueryApi.getRecentCompletionRate(profile.getUserId(), 7);
            List<HealthMetricDTO> metricHistory = healthMetricQueryApi.listMetricsByUserId(
                    profile.getUserId(), null, null);

            item.setPlanTitle(planTitle);
            item.setInterventionSummary(SimilarCaseReferenceBuilder.buildInterventionSummary(
                    planTitle, planSummary, completionRate));
            item.setOutcomeSummary(SimilarCaseReferenceBuilder.buildOutcomeSummary(metricHistory));
            if (completionRate != null) {
                item.setCheckinCompletionRate(completionRate.setScale(0, RoundingMode.HALF_UP).intValue());
            }
            item.setSummary(SimilarCaseReferenceBuilder.buildReferenceSummary(
                    item.getInterventionSummary(), item.getOutcomeSummary()));
            candidates.add(item);
        }

        candidates.sort(Comparator.comparing(SimilarCaseItem::getSimilarityScore).reversed());
        return candidates.stream().limit(topK).toList();
    }

    private int normalizeSimilarCaseLimit(Integer limit, int defaultLimit, int maxLimit) {
        if (limit == null || limit < 1) {
            return defaultLimit;
        }
        return Math.min(limit, maxLimit);
    }

    private RiskTrendResponse buildRiskTrend(Integer userId) {
        LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAssessment::getUserId, userId)
                .eq(RiskAssessment::getCallStatus, "success")
                .orderByAsc(RiskAssessment::getCreateTime);

        List<RiskTrendResponse.RiskTrendPoint> points = riskAssessmentMapper.selectList(wrapper).stream()
                .map(record -> {
                    RiskTrendResponse.RiskTrendPoint point = new RiskTrendResponse.RiskTrendPoint();
                    point.setAssessmentId(record.getAssessmentId());
                    point.setRecordedAt(record.getCreateTime());
                    point.setRiskScore(record.getRiskScore());
                    point.setRiskLevel(record.getRiskLevel());
                    if (StringUtils.hasText(record.getResponseResult())) {
                        try {
                            ParsedRiskResult parsed = RiskResultParser.parse(record.getResponseResult());
                            point.setRiskScore(parsed.getRiskScore());
                            point.setRiskLevel(parsed.getRiskLevel());
                        } catch (Exception ignored) {
                            // keep stored values
                        }
                    }
                    return point;
                })
                .filter(point -> point.getRiskScore() != null)
                .toList();

        RiskTrendResponse response = new RiskTrendResponse();
        response.setPoints(points);
        return response;
    }

    private HealthMetricDTO resolveMetricForAssessment(RiskAssessment assessment) {
        if (assessment.getMetricId() != null) {
            HealthMetric metric = healthMetricMapper.selectById(assessment.getMetricId());
            if (metric != null) {
                return toMetricDto(metric);
            }
        }
        return healthMetricQueryApi.getLatestMetricByUserId(assessment.getUserId());
    }

    private HealthMetricDTO toMetricDto(HealthMetric metric) {
        HealthMetricDTO dto = new HealthMetricDTO();
        dto.setMetricId(metric.getMetricId());
        dto.setUserId(metric.getUserId());
        dto.setWeightKg(metric.getWeightKg());
        dto.setWaistCm(metric.getWaistCm());
        dto.setSystolicBp(metric.getSystolicBp());
        dto.setDiastolicBp(metric.getDiastolicBp());
        dto.setFastingGlucose(metric.getFastingGlucose());
        dto.setPostprandialGlucose(metric.getPostprandialGlucose());
        dto.setHba1c(metric.getHba1c());
        dto.setDietStatus(metric.getDietStatus());
        dto.setExerciseStatus(metric.getExerciseStatus());
        dto.setRecordedAt(metric.getRecordedAt() == null ? null : metric.getRecordedAt().toLocalDate());
        dto.setCreateTime(metric.getCreateTime());
        return dto;
    }

    @Override
    public RiskAssessmentDTO getLatestAssessmentByUserId(Integer userId) {
        RiskAssessment assessment = findLatestSuccessByUserId(userId);
        return assessment == null ? null : toContractDto(assessment);
    }

    @Override
    public List<RiskAssessmentDTO> listAssessmentsByUserId(Integer userId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<RiskAssessment> wrapper = buildUserDateWrapper(userId, startDate, endDate);
        wrapper.orderByDesc(RiskAssessment::getCreateTime);
        return riskAssessmentMapper.selectList(wrapper).stream()
                .map(this::toContractDto)
                .toList();
    }

    @Override
    public String getLatestRiskSummaryByUserId(Integer userId) {
        RiskAssessment assessment = findLatestSuccessByUserId(userId);
        if (assessment == null) {
            return null;
        }
        if (StringUtils.hasText(assessment.getSummary())) {
            return assessment.getSummary();
        }
        try {
            ParsedRiskResult parsed = RiskResultParser.parse(assessment.getResponseResult());
            return parsed.getSummary();
        } catch (Exception exception) {
            return assessment.getRequestSummary();
        }
    }

    private List<String> collectMissing(Integer userId) {
        List<String> missing = new ArrayList<>();
        if (!patientProfileQueryApi.hasProfile(userId)) {
            missing.add("patient_profile");
        }
        if (healthMetricQueryApi.getLatestMetricByUserId(userId) == null) {
            missing.add("latest_health_metric");
        }
        return missing;
    }

    private Map<String, Object> buildDifyInputs(PatientProfileDTO profile, HealthMetricDTO metric) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("user_id", stringify(profile.getUserId()));
        inputs.put("age", stringify(profile.getAge()));
        inputs.put("gender", stringify(profile.getGender()));
        inputs.put("height_cm", stringify(profile.getHeightCm()));
        inputs.put("weight_kg", stringify(metric.getWeightKg()));
        BigDecimal bmi = MetricAbnormalUtils.calculateBmi(profile.getHeightCm(), metric.getWeightKg());
        if (bmi != null) {
            inputs.put("bmi", stringify(bmi));
        }
        inputs.put("waist_cm", stringify(metric.getWaistCm() != null ? metric.getWaistCm() : profile.getBaseWaistCm()));
        inputs.put("systolic_bp", stringify(metric.getSystolicBp()));
        inputs.put("diastolic_bp", stringify(metric.getDiastolicBp()));
        inputs.put("fasting_glucose", stringify(metric.getFastingGlucose()));
        inputs.put("postprandial_glucose", stringify(metric.getPostprandialGlucose()));
        inputs.put("hba1c", stringify(metric.getHba1c()));
        inputs.put("family_history", stringify(profile.getFamilyHistory()));
        inputs.put("chronic_history", stringify(profile.getChronicHistory()));
        inputs.put("diet_status", stringify(metric.getDietStatus()));
        inputs.put("exercise_status", stringify(metric.getExerciseStatus()));
        inputs.put("profile_summary", stringify(profile.getProfileSummary()));
        inputs.put("latest_metric", stringify(metric.getMetricSummary()));
        inputs.put("health_context", buildRequestSummary(profile, metric, inputs));
        return inputs;
    }

    private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        if (cause != null && StringUtils.hasText(cause.getMessage())) {
            message = (StringUtils.hasText(message) ? message + ": " : "") + cause.getMessage();
        }
        return StringUtils.hasText(message) ? message : exception.getClass().getSimpleName();
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String buildRequestSummary(PatientProfileDTO profile, HealthMetricDTO metric,
                                       Map<String, Object> inputs) {
        return "档案：" + profile.getProfileSummary() + "；最新指标：" + metric.getMetricSummary()
                + "；BMI：" + inputs.get("bmi");
    }

    private boolean shouldUseDevMock() {
        String apiKey = difyProperties.getRiskPredictApiKey();
        return apiKey == null || apiKey.isBlank() || apiKey.startsWith("your_");
    }

    private String buildDevMockResponse() {
        return """
                {
                  "risk_level": "medium",
                  "risk_score": 65,
                  "diabetes_type_tendency": "2型倾向",
                  "main_risk_factors": ["空腹血糖偏高", "BMI超标"],
                  "indicator_analysis": "当前指标显示存在糖尿病前期风险，请结合线下检查进一步确认。",
                  "health_advice": "控制主食摄入，保持规律运动，定期监测血糖。",
                  "medical_warning": "建议尽快到内分泌科复查空腹血糖和糖化血红蛋白。",
                  "summary": "目前处于中等风险，需要加强生活方式管理。",
                  "reference_sources": ["筛查诊断指标与化验单解释", "风险因素预防与生活方式干预", "控糖饮食总原则与餐盘法"]
                }
                """;
    }

    private ParsedRiskResult completeParsedResult(ParsedRiskResult parsed, PatientProfileDTO profile, HealthMetricDTO metric) {
        if (parsed == null) {
            parsed = new ParsedRiskResult();
        }

        BigDecimal bmi = MetricAbnormalUtils.calculateBmi(profile.getHeightCm(), metric.getWeightKg());
        List<String> factors = new ArrayList<>(parsed.getMainRiskFactors() == null ? List.of() : parsed.getMainRiskFactors());
        int score = parsed.getRiskScore() == null ? 20 : parsed.getRiskScore();

        if (metric.getFastingGlucose() != null && metric.getFastingGlucose().compareTo(new BigDecimal("7.0")) >= 0) {
            score += 26;
            addFactor(factors, "空腹血糖达到糖尿病风险复查阈值");
        } else if (metric.getFastingGlucose() != null && metric.getFastingGlucose().compareTo(new BigDecimal("6.1")) >= 0) {
            score += 18;
            addFactor(factors, "空腹血糖偏高");
        }

        if (metric.getPostprandialGlucose() != null && metric.getPostprandialGlucose().compareTo(new BigDecimal("11.1")) >= 0) {
            score += 24;
            addFactor(factors, "餐后血糖达到糖尿病风险复查阈值");
        } else if (metric.getPostprandialGlucose() != null && metric.getPostprandialGlucose().compareTo(new BigDecimal("7.8")) >= 0) {
            score += 14;
            addFactor(factors, "餐后血糖偏高");
        }

        if (metric.getHba1c() != null && metric.getHba1c().compareTo(new BigDecimal("6.5")) >= 0) {
            score += 24;
            addFactor(factors, "糖化血红蛋白达到复查阈值");
        } else if (metric.getHba1c() != null && metric.getHba1c().compareTo(new BigDecimal("5.7")) >= 0) {
            score += 14;
            addFactor(factors, "糖化血红蛋白偏高");
        }

        if (bmi != null && bmi.compareTo(new BigDecimal("28")) >= 0) {
            score += 14;
            addFactor(factors, "BMI达到肥胖范围");
        } else if (bmi != null && bmi.compareTo(new BigDecimal("24")) >= 0) {
            score += 8;
            addFactor(factors, "BMI偏高");
        }

        if (metric.getSystolicBp() != null && metric.getSystolicBp() >= 140
                || metric.getDiastolicBp() != null && metric.getDiastolicBp() >= 90) {
            score += 10;
            addFactor(factors, "血压偏高");
        }

        if (StringUtils.hasText(profile.getFamilyHistory()) && profile.getFamilyHistory().contains("糖尿病")) {
            score += 10;
            addFactor(factors, "存在糖尿病家族史");
        }

        score = Math.max(0, Math.min(score, 100));
        if (!StringUtils.hasText(parsed.getRiskLevel())) {
            parsed.setRiskLevel(score >= 75 ? "high" : score >= 45 ? "medium" : "low");
        }
        if (parsed.getRiskScore() == null) {
            parsed.setRiskScore(score);
        }
        if (!StringUtils.hasText(parsed.getDiabetesTypeTendency())) {
            parsed.setDiabetesTypeTendency("2型糖尿病风险倾向，需结合线下复查确认");
        }
        if (factors.isEmpty()) {
            addFactor(factors, "当前风险因素不突出，建议继续保持监测");
        }
        parsed.setMainRiskFactors(factors);
        if (!StringUtils.hasText(parsed.getIndicatorAnalysis())) {
            parsed.setIndicatorAnalysis(buildIndicatorAnalysis(profile, metric, bmi));
        }
        if (!StringUtils.hasText(parsed.getHealthAdvice())) {
            parsed.setHealthAdvice("建议控制精制碳水和含糖饮品摄入，增加餐后轻中强度活动，保持规律睡眠，并连续记录空腹血糖、餐后血糖和体重变化。");
        }
        if (!StringUtils.hasText(parsed.getMedicalWarning())) {
            parsed.setMedicalWarning("若空腹血糖多次达到或超过7.0 mmol/L、餐后血糖达到或超过11.1 mmol/L，或伴随明显口渴、多尿、体重下降等症状，请尽快到内分泌科复查。");
        }
        if (!StringUtils.hasText(parsed.getSummary())) {
            parsed.setSummary(buildRiskSummary(parsed.getRiskLevel(), parsed.getRiskScore(), factors));
        }
        if (parsed.getReferenceSources() == null || parsed.getReferenceSources().isEmpty()) {
            parsed.setReferenceSources(List.of(
                    "糖尿病筛查诊断指标与化验单解释",
                    "风险因素预防与生活方式干预"));
        }
        return parsed;
    }

    private void addFactor(List<String> factors, String factor) {
        if (!factors.contains(factor)) {
            factors.add(factor);
        }
    }

    private String buildIndicatorAnalysis(PatientProfileDTO profile, HealthMetricDTO metric, BigDecimal bmi) {
        return "本次评估结合年龄" + stringify(profile.getAge())
                + "岁、性别" + GenderUtils.toDisplayLabel(profile.getGender())
                + "、BMI " + stringify(bmi)
                + "、空腹血糖" + stringify(metric.getFastingGlucose()) + " mmol/L"
                + "、餐后血糖" + stringify(metric.getPostprandialGlucose()) + " mmol/L"
                + "、糖化血红蛋白" + stringify(metric.getHba1c()) + "%"
                + "和血压" + stringify(metric.getSystolicBp()) + "/" + stringify(metric.getDiastolicBp())
                + "综合判断，结果仅用于健康管理参考，不能替代线下诊断。";
    }

    private String buildRiskSummary(String riskLevel, Integer riskScore, List<String> factors) {
        String label = switch (riskLevel == null ? "" : riskLevel) {
            case "high" -> "高风险";
            case "low" -> "低风险";
            default -> "中等风险";
        };
        return "本次评估为" + label + "，综合评分" + stringify(riskScore)
                + "。主要关注：" + String.join("、", factors.stream().limit(3).toList()) + "。";
    }

    private RiskAssessment findLatestByUserId(Integer userId) {
        return riskAssessmentMapper.selectOne(new LambdaQueryWrapper<RiskAssessment>()
                .eq(RiskAssessment::getUserId, userId)
                .orderByDesc(RiskAssessment::getCreateTime)
                .last("LIMIT 1"));
    }

    private RiskAssessment findLatestSuccessByUserId(Integer userId) {
        return riskAssessmentMapper.selectOne(new LambdaQueryWrapper<RiskAssessment>()
                .eq(RiskAssessment::getUserId, userId)
                .eq(RiskAssessment::getCallStatus, "success")
                .orderByDesc(RiskAssessment::getCreateTime)
                .last("LIMIT 1"));
    }

    private RiskAssessment requireOwnedAssessment(Integer assessmentId, Integer userId) {
        RiskAssessment assessment = riskAssessmentMapper.selectById(assessmentId);
        if (assessment == null || !assessment.getUserId().equals(userId)) {
            throw new BusinessException(404, "评估记录不存在");
        }
        return assessment;
    }

    private LambdaQueryWrapper<RiskAssessment> buildUserDateWrapper(Integer userId,
                                                                    LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAssessment::getUserId, userId);
        applyDateRange(wrapper, startDate, endDate);
        return wrapper;
    }

    private void applyDateRange(LambdaQueryWrapper<RiskAssessment> wrapper,
                                LocalDate startDate, LocalDate endDate) {
        if (startDate != null) {
            wrapper.ge(RiskAssessment::getCreateTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(RiskAssessment::getCreateTime, endDate.atTime(LocalTime.MAX));
        }
    }

    private RiskPredictResponse toPredictResponse(RiskAssessment assessment, ParsedRiskResult parsed) {
        RiskPredictResponse response = new RiskPredictResponse();
        response.setAssessmentId(assessment.getAssessmentId());
        response.setRiskLevel(parsed.getRiskLevel());
        response.setRiskScore(parsed.getRiskScore());
        response.setDiabetesTypeTendency(parsed.getDiabetesTypeTendency());
        response.setMainRiskFactors(parsed.getMainRiskFactors());
        response.setIndicatorAnalysis(parsed.getIndicatorAnalysis());
        response.setHealthAdvice(parsed.getHealthAdvice());
        response.setMedicalWarning(parsed.getMedicalWarning());
        response.setSummary(parsed.getSummary());
        response.setReferenceSources(parsed.getReferenceSources());
        response.setCallStatus(assessment.getCallStatus());
        response.setCreateTime(assessment.getCreateTime());
        return response;
    }

    private RiskDetailResponse toDetailResponse(RiskAssessment assessment) {
        RiskDetailResponse response = new RiskDetailResponse();
        response.setAssessmentId(assessment.getAssessmentId());
        response.setUserId(assessment.getUserId());
        response.setRiskLevel(assessment.getRiskLevel());
        response.setRiskScore(assessment.getRiskScore());
        response.setDiabetesTypeTendency(assessment.getDiabetesTypeTendency());
        response.setMainRiskFactors(splitStoredList(assessment.getMainRiskFactors()));
        response.setIndicatorAnalysis(GenderUtils.normalizeGenderLabelInText(assessment.getIndicatorAnalysis()));
        response.setHealthAdvice(assessment.getHealthAdvice());
        response.setMedicalWarning(assessment.getMedicalWarning());
        response.setSummary(assessment.getSummary());
        response.setRequestSummary(formatRequestSummaryForDisplay(assessment.getRequestSummary()));
        response.setCallStatus(assessment.getCallStatus());
        response.setErrorMessage(assessment.getErrorMessage());
        response.setCreateTime(assessment.getCreateTime());

        if (StringUtils.hasText(assessment.getResponseResult())) {
            try {
                ParsedRiskResult parsed = RiskResultParser.parse(assessment.getResponseResult());
                mergeParsedIntoDetail(response, parsed);
            } catch (Exception ignored) {
                if (!StringUtils.hasText(response.getSummary())) {
                    response.setSummary(assessment.getRequestSummary());
                }
            }
        }
        return response;
    }

    /** Prefer parsed AI fields when present; keep DB columns as fallback for seed / legacy rows. */
    private void mergeParsedIntoDetail(RiskDetailResponse response, ParsedRiskResult parsed) {
        if (StringUtils.hasText(parsed.getRiskLevel())) {
            response.setRiskLevel(parsed.getRiskLevel());
        }
        if (parsed.getRiskScore() != null) {
            response.setRiskScore(parsed.getRiskScore());
        }
        if (StringUtils.hasText(parsed.getDiabetesTypeTendency())) {
            response.setDiabetesTypeTendency(parsed.getDiabetesTypeTendency());
        }
        if (parsed.getMainRiskFactors() != null && !parsed.getMainRiskFactors().isEmpty()) {
            response.setMainRiskFactors(parsed.getMainRiskFactors());
        }
        if (StringUtils.hasText(parsed.getIndicatorAnalysis())) {
            response.setIndicatorAnalysis(GenderUtils.normalizeGenderLabelInText(parsed.getIndicatorAnalysis()));
        }
        if (StringUtils.hasText(parsed.getHealthAdvice())) {
            response.setHealthAdvice(parsed.getHealthAdvice());
        }
        if (StringUtils.hasText(parsed.getMedicalWarning())) {
            response.setMedicalWarning(parsed.getMedicalWarning());
        }
        if (StringUtils.hasText(parsed.getSummary())) {
            response.setSummary(parsed.getSummary());
        }
        if (parsed.getReferenceSources() != null && !parsed.getReferenceSources().isEmpty()) {
            response.setReferenceSources(parsed.getReferenceSources());
        }
        response.setCallStatus("success");
        response.setErrorMessage(null);
    }

    private String formatRequestSummaryForDisplay(String summary) {
        return GenderUtils.normalizeGenderLabelInText(summary);
    }

    private RiskHistoryItem toHistoryItem(RiskAssessment assessment) {
        RiskHistoryItem item = new RiskHistoryItem();
        item.setAssessmentId(assessment.getAssessmentId());
        item.setRiskLevel(assessment.getRiskLevel());
        item.setRiskScore(assessment.getRiskScore());
        item.setSummary(assessment.getSummary());
        item.setCallStatus(assessment.getCallStatus());
        item.setCreateTime(assessment.getCreateTime());
        if (StringUtils.hasText(assessment.getResponseResult())) {
            try {
                ParsedRiskResult parsed = RiskResultParser.parse(assessment.getResponseResult());
                if (StringUtils.hasText(parsed.getRiskLevel())) {
                    item.setRiskLevel(parsed.getRiskLevel());
                }
                if (parsed.getRiskScore() != null) {
                    item.setRiskScore(parsed.getRiskScore());
                }
                if (StringUtils.hasText(parsed.getSummary())) {
                    item.setSummary(parsed.getSummary());
                }
                item.setCallStatus("success");
            } catch (Exception ignored) {
                if (!StringUtils.hasText(item.getSummary())) {
                    item.setSummary(assessment.getRequestSummary());
                }
            }
        } else if (!StringUtils.hasText(item.getSummary())) {
            item.setSummary(assessment.getRequestSummary());
        }
        return item;
    }

    private AdminRiskListItem toAdminListItem(RiskAssessment assessment) {
        AdminRiskListItem item = new AdminRiskListItem();
        item.setAssessmentId(assessment.getAssessmentId());
        item.setUserId(assessment.getUserId());
        item.setRiskLevel(assessment.getRiskLevel());
        item.setRiskScore(assessment.getRiskScore());
        item.setCallStatus(assessment.getCallStatus());
        item.setCreateTime(assessment.getCreateTime());

        UserBasicDTO user = userQueryApi.getUserBasicById(assessment.getUserId());
        if (user != null) {
            item.setUsername(user.getUsername());
        }

        RiskHistoryItem historyItem = toHistoryItem(assessment);
        item.setRiskLevel(historyItem.getRiskLevel());
        item.setRiskScore(historyItem.getRiskScore());
        item.setSummary(historyItem.getSummary());
        return item;
    }

    private RiskAssessmentDTO toContractDto(RiskAssessment assessment) {
        RiskAssessmentDTO dto = new RiskAssessmentDTO();
        dto.setAssessmentId(assessment.getAssessmentId());
        dto.setUserId(assessment.getUserId());
        dto.setMetricId(assessment.getMetricId());
        dto.setRiskLevel(assessment.getRiskLevel());
        dto.setRiskScore(assessment.getRiskScore());
        dto.setDiabetesTypeTendency(assessment.getDiabetesTypeTendency());
        dto.setMainRiskFactors(assessment.getMainRiskFactors());
        dto.setIndicatorAnalysis(assessment.getIndicatorAnalysis());
        dto.setHealthAdvice(assessment.getHealthAdvice());
        dto.setMedicalWarning(assessment.getMedicalWarning());
        dto.setSummary(assessment.getSummary());
        dto.setCallStatus(assessment.getCallStatus());
        dto.setErrorMessage(assessment.getErrorMessage());
        dto.setCreateTime(assessment.getCreateTime());

        if (StringUtils.hasText(assessment.getResponseResult())) {
            try {
                ParsedRiskResult parsed = RiskResultParser.parse(assessment.getResponseResult());
                dto.setRiskLevel(parsed.getRiskLevel());
                dto.setRiskScore(parsed.getRiskScore());
                dto.setDiabetesTypeTendency(parsed.getDiabetesTypeTendency());
                dto.setMainRiskFactors(joinList(parsed.getMainRiskFactors()));
                dto.setIndicatorAnalysis(parsed.getIndicatorAnalysis());
                dto.setHealthAdvice(parsed.getHealthAdvice());
                dto.setMedicalWarning(parsed.getMedicalWarning());
                dto.setSummary(parsed.getSummary());
                dto.setCallStatus("success");
                dto.setErrorMessage(null);
            } catch (Exception ignored) {
                dto.setSummary(assessment.getRequestSummary());
            }
        }
        return dto;
    }

    private void applyParsedResult(RiskAssessment assessment, ParsedRiskResult parsed) {
        assessment.setRiskLevel(parsed.getRiskLevel());
        assessment.setRiskScore(parsed.getRiskScore());
        assessment.setDiabetesTypeTendency(parsed.getDiabetesTypeTendency());
        assessment.setMainRiskFactors(joinList(parsed.getMainRiskFactors()));
        assessment.setIndicatorAnalysis(parsed.getIndicatorAnalysis());
        assessment.setHealthAdvice(parsed.getHealthAdvice());
        assessment.setMedicalWarning(parsed.getMedicalWarning());
        assessment.setSummary(parsed.getSummary());
    }

    private List<String> splitStoredList(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(";"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String joinList(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join("; ", values);
    }
}
