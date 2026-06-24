package com.diabetes.assistant.modules.risk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.PageUtils;
import com.diabetes.assistant.modules.dify.service.DifyService;
import com.diabetes.assistant.modules.healthmetric.contract.HealthMetricQueryApi;
import com.diabetes.assistant.modules.healthmetric.contract.dto.HealthMetricDTO;
import com.diabetes.assistant.modules.healthmetric.util.MetricAbnormalUtils;
import com.diabetes.assistant.modules.profile.contract.PatientProfileQueryApi;
import com.diabetes.assistant.modules.profile.contract.dto.PatientProfileDTO;
import com.diabetes.assistant.modules.risk.contract.RiskAssessmentQueryApi;
import com.diabetes.assistant.modules.risk.contract.dto.RiskAssessmentDTO;
import com.diabetes.assistant.modules.risk.dto.AdminRiskListItem;
import com.diabetes.assistant.modules.risk.dto.RiskDetailResponse;
import com.diabetes.assistant.modules.risk.dto.RiskEntryResponse;
import com.diabetes.assistant.modules.risk.dto.RiskHistoryItem;
import com.diabetes.assistant.modules.risk.dto.RiskPredictResponse;
import com.diabetes.assistant.modules.risk.entity.RiskAssessment;
import com.diabetes.assistant.modules.risk.mapper.RiskAssessmentMapper;
import com.diabetes.assistant.modules.risk.service.RiskService;
import com.diabetes.assistant.modules.risk.util.RiskResultParser;
import com.diabetes.assistant.modules.risk.util.RiskResultParser.ParsedRiskResult;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import com.diabetes.assistant.modules.user.contract.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiskServiceImpl implements RiskService, RiskAssessmentQueryApi {

    private final RiskAssessmentMapper riskAssessmentMapper;
    private final PatientProfileQueryApi patientProfileQueryApi;
    private final HealthMetricQueryApi healthMetricQueryApi;
    private final DifyService difyService;
    private final UserQueryApi userQueryApi;

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
        String rawResponse;
        ParsedRiskResult parsed;
        String callStatus;
        String errorMessage = null;

        try {
            rawResponse = difyService.callRiskPrediction(inputs, String.valueOf(userId));
            if (rawResponse.startsWith("TODO")) {
                rawResponse = buildDevMockResponse();
            }
            parsed = RiskResultParser.parse(rawResponse);
            callStatus = "success";
        } catch (Exception exception) {
            callStatus = "failed";
            errorMessage = exception.getMessage();
            rawResponse = null;
            parsed = null;

            RiskAssessment failed = new RiskAssessment();
            failed.setUserId(userId);
            failed.setRequestSummary(requestSummary);
            failed.setCallStatus(callStatus);
            failed.setErrorMessage(errorMessage);
            riskAssessmentMapper.insert(failed);

            throw new BusinessException(502, "AI服务调用失败，请稍后重试",
                    Map.of("error_message", errorMessage == null ? "未知错误" : errorMessage));
        }

        RiskAssessment assessment = new RiskAssessment();
        assessment.setUserId(userId);
        assessment.setRequestSummary(requestSummary);
        assessment.setResponseResult(rawResponse);
        assessment.setRiskLevel(parsed.getRiskLevel());
        assessment.setRiskScore(parsed.getRiskScore());
        assessment.setCallStatus(callStatus);
        riskAssessmentMapper.insert(assessment);

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
        inputs.put("user_id", profile.getUserId());
        inputs.put("age", profile.getAge());
        inputs.put("gender", profile.getGender());
        inputs.put("height_cm", profile.getHeightCm());
        inputs.put("weight_kg", metric.getWeightKg());
        BigDecimal bmi = MetricAbnormalUtils.calculateBmi(profile.getHeightCm(), metric.getWeightKg());
        if (bmi != null) {
            inputs.put("bmi", bmi);
        }
        inputs.put("waist_cm", metric.getWaistCm() != null ? metric.getWaistCm() : profile.getBaseWaistCm());
        inputs.put("systolic_bp", metric.getSystolicBp());
        inputs.put("diastolic_bp", metric.getDiastolicBp());
        inputs.put("fasting_glucose", metric.getFastingGlucose());
        inputs.put("postprandial_glucose", metric.getPostprandialGlucose());
        inputs.put("hba1c", metric.getHba1c());
        inputs.put("family_history", profile.getFamilyHistory());
        inputs.put("chronic_history", profile.getChronicHistory());
        inputs.put("diet_status", metric.getDietStatus());
        inputs.put("exercise_status", metric.getExerciseStatus());
        inputs.put("profile_summary", profile.getProfileSummary());
        inputs.put("latest_metric", metric.getMetricSummary());
        return inputs;
    }

    private String buildRequestSummary(PatientProfileDTO profile, HealthMetricDTO metric,
                                       Map<String, Object> inputs) {
        return "档案：" + profile.getProfileSummary() + "；最新指标：" + metric.getMetricSummary()
                + "；BMI：" + inputs.get("bmi");
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
                  "summary": "目前处于中等风险，需要加强生活方式管理。"
                }
                """;
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
        response.setCallStatus(assessment.getCallStatus());
        response.setCreateTime(assessment.getCreateTime());
        return response;
    }

    private RiskDetailResponse toDetailResponse(RiskAssessment assessment) {
        RiskDetailResponse response = new RiskDetailResponse();
        response.setAssessmentId(assessment.getAssessmentId());
        response.setRiskLevel(assessment.getRiskLevel());
        response.setRiskScore(assessment.getRiskScore());
        response.setRequestSummary(assessment.getRequestSummary());
        response.setCallStatus(assessment.getCallStatus());
        response.setErrorMessage(assessment.getErrorMessage());
        response.setCreateTime(assessment.getCreateTime());

        if (StringUtils.hasText(assessment.getResponseResult())) {
            try {
                ParsedRiskResult parsed = RiskResultParser.parse(assessment.getResponseResult());
                response.setDiabetesTypeTendency(parsed.getDiabetesTypeTendency());
                response.setMainRiskFactors(parsed.getMainRiskFactors());
                response.setIndicatorAnalysis(parsed.getIndicatorAnalysis());
                response.setHealthAdvice(parsed.getHealthAdvice());
                response.setMedicalWarning(parsed.getMedicalWarning());
                response.setSummary(parsed.getSummary());
            } catch (Exception ignored) {
                response.setSummary(assessment.getRequestSummary());
            }
        }
        return response;
    }

    private RiskHistoryItem toHistoryItem(RiskAssessment assessment) {
        RiskHistoryItem item = new RiskHistoryItem();
        item.setAssessmentId(assessment.getAssessmentId());
        item.setRiskLevel(assessment.getRiskLevel());
        item.setRiskScore(assessment.getRiskScore());
        item.setCallStatus(assessment.getCallStatus());
        item.setCreateTime(assessment.getCreateTime());
        if (StringUtils.hasText(assessment.getResponseResult())) {
            try {
                item.setSummary(RiskResultParser.parse(assessment.getResponseResult()).getSummary());
            } catch (Exception ignored) {
                item.setSummary(assessment.getRequestSummary());
            }
        } else {
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
        item.setSummary(historyItem.getSummary());
        return item;
    }

    private RiskAssessmentDTO toContractDto(RiskAssessment assessment) {
        RiskAssessmentDTO dto = new RiskAssessmentDTO();
        dto.setAssessmentId(assessment.getAssessmentId());
        dto.setUserId(assessment.getUserId());
        dto.setRiskLevel(assessment.getRiskLevel());
        dto.setRiskScore(assessment.getRiskScore());
        dto.setCallStatus(assessment.getCallStatus());
        dto.setErrorMessage(assessment.getErrorMessage());
        dto.setCreateTime(assessment.getCreateTime());

        if (StringUtils.hasText(assessment.getResponseResult())) {
            try {
                ParsedRiskResult parsed = RiskResultParser.parse(assessment.getResponseResult());
                dto.setDiabetesTypeTendency(parsed.getDiabetesTypeTendency());
                dto.setMainRiskFactors(joinList(parsed.getMainRiskFactors()));
                dto.setIndicatorAnalysis(parsed.getIndicatorAnalysis());
                dto.setHealthAdvice(parsed.getHealthAdvice());
                dto.setMedicalWarning(parsed.getMedicalWarning());
                dto.setSummary(parsed.getSummary());
            } catch (Exception ignored) {
                dto.setSummary(assessment.getRequestSummary());
            }
        }
        return dto;
    }

    private String joinList(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join("；", values);
    }
}
