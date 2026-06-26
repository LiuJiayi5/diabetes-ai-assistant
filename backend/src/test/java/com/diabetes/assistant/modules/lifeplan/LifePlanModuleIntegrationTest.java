package com.diabetes.assistant.modules.lifeplan;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.assistant.common.constants.RoleConstants;
import com.diabetes.assistant.common.constants.StatusConstants;
import com.diabetes.assistant.common.utils.JwtUtil;
import com.diabetes.assistant.common.utils.PasswordUtil;
import com.diabetes.assistant.modules.dify.dto.DifyWorkflowResult;
import com.diabetes.assistant.modules.dify.service.DifyService;
import com.diabetes.assistant.modules.lifeplan.entity.HealthMetricSnapshot;
import com.diabetes.assistant.modules.lifeplan.entity.LifePlan;
import com.diabetes.assistant.modules.lifeplan.entity.PatientProfileSnapshot;
import com.diabetes.assistant.modules.lifeplan.entity.RiskAssessmentSnapshot;
import com.diabetes.assistant.modules.lifeplan.mapper.HealthMetricSnapshotMapper;
import com.diabetes.assistant.modules.lifeplan.mapper.LifePlanMapper;
import com.diabetes.assistant.modules.lifeplan.mapper.PatientProfileSnapshotMapper;
import com.diabetes.assistant.modules.lifeplan.mapper.RiskAssessmentSnapshotMapper;
import com.diabetes.assistant.modules.user.entity.User;
import com.diabetes.assistant.modules.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LifePlanModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PatientProfileSnapshotMapper profileMapper;
    @Autowired
    private HealthMetricSnapshotMapper metricMapper;
    @Autowired
    private RiskAssessmentSnapshotMapper assessmentMapper;
    @Autowired
    private LifePlanMapper lifePlanMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DifyService difyService;

    private Integer patientId;
    private Integer otherPatientId;
    private Integer adminId;

    @BeforeEach
    void setUp() {
        lifePlanMapper.delete(null);
        assessmentMapper.delete(null);
        metricMapper.delete(null);
        profileMapper.delete(null);
        userMapper.delete(null);

        patientId = insertUser("patient_lp", "13810000001", RoleConstants.PATIENT);
        otherPatientId = insertUser("other_lp", "13810000002", RoleConstants.PATIENT);
        adminId = insertUser("admin_lp", "13810000003", RoleConstants.ADMIN);
    }

    @Test
    void generateWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/ai/life-plan/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void missingProfileReturns400WithMissingData() throws Exception {
        insertMetric(patientId, new BigDecimal("72.5"));
        insertAssessment(patientId, "medium", "success");

        mockMvc.perform(postGenerate(patientId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.missing[0]").value("patient_profile"));
    }

    @Test
    void missingLatestMetricReturns400() throws Exception {
        insertProfile(patientId);
        insertAssessment(patientId, "medium", "success");

        mockMvc.perform(postGenerate(patientId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.missing[0]").value("latest_health_metric"));
    }

    @Test
    void missingSuccessfulRiskAssessmentReturns400() throws Exception {
        insertProfile(patientId);
        insertMetric(patientId, new BigDecimal("72.5"));
        insertAssessment(patientId, "medium", "failed");

        mockMvc.perform(postGenerate(patientId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.missing[0]").value("latest_risk_assessment"));
    }

    @Test
    void ignoresFrontendUserIdAndUsesTokenUser() throws Exception {
        seedGenerationData(patientId);
        seedGenerationData(otherPatientId);
        when(difyService.callLifePlan(any(), eq("user-" + patientId))).thenReturn(successResult());

        mockMvc.perform(post("/api/ai/life-plan/generate")
                        .header("Authorization", bearerToken(patientId, "patient_lp", RoleConstants.PATIENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":" + otherPatientId + ",\"plan_goal\":\"控糖\",\"avoid_items\":[],\"plan_days\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(patientId));

        assertThat(lifePlanMapper.selectList(new LambdaQueryWrapper<LifePlan>().eq(LifePlan::getUserId, otherPatientId))).isEmpty();
    }

    @Test
    void difySuccessSavesActivePlan() throws Exception {
        seedGenerationData(patientId);
        when(difyService.callLifePlan(any(), eq("user-" + patientId))).thenReturn(successResult());

        mockMvc.perform(postGenerate(patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.call_status").value("success"))
                .andExpect(jsonPath("$.data.plan_json.diet_plan.breakfast").value("燕麦粥"));

        LifePlan plan = latestPlan(patientId);
        assertThat(plan.getPlanJson()).contains("diet_plan");
        assertThat(plan.getCheckinTasksJson()).contains("饮食打卡");
    }

    @Test
    void newSuccessMovesOldActiveToHistory() throws Exception {
        seedGenerationData(patientId);
        insertLifePlan(patientId, "active", "success", "旧方案");
        when(difyService.callLifePlan(any(), eq("user-" + patientId))).thenReturn(successResult());

        mockMvc.perform(postGenerate(patientId)).andExpect(status().isOk());

        List<LifePlan> plans = lifePlanMapper.selectList(new LambdaQueryWrapper<LifePlan>().eq(LifePlan::getUserId, patientId));
        assertThat(plans).filteredOn(plan -> "active".equals(plan.getStatus())).hasSize(1);
        assertThat(plans).filteredOn(plan -> "history".equals(plan.getStatus())).isNotEmpty();
    }

    @Test
    void difyFalseReturns502AndKeepsOldActive() throws Exception {
        seedGenerationData(patientId);
        LifePlan old = insertLifePlan(patientId, "active", "success", "旧方案");
        when(difyService.callLifePlan(any(), eq("user-" + patientId))).thenReturn(new DifyWorkflowResult(Map.of(
                "success", false,
                "error_message", "workflow failed"
        )));

        mockMvc.perform(postGenerate(patientId))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(502));

        assertThat(lifePlanMapper.selectById(old.getPlanId()).getStatus()).isEqualTo("active");
        assertThat(lifePlanMapper.selectList(new LambdaQueryWrapper<LifePlan>().eq(LifePlan::getCallStatus, "failed"))).hasSize(1);
    }

    @Test
    void invalidDifyFormatReturns502() throws Exception {
        seedGenerationData(patientId);
        when(difyService.callLifePlan(any(), eq("user-" + patientId))).thenReturn(new DifyWorkflowResult(Map.of("success", true)));

        mockMvc.perform(postGenerate(patientId))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(502));
    }

    @Test
    void listOnlyReturnsCurrentUserPlans() throws Exception {
        insertLifePlan(patientId, "active", "success", "我的方案");
        insertLifePlan(otherPatientId, "active", "success", "别人方案");

        mockMvc.perform(get("/api/life-plans")
                        .header("Authorization", bearerToken(patientId, "patient_lp", RoleConstants.PATIENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].user_id").value(patientId));
    }

    @Test
    void detailCannotReadOtherUserPlan() throws Exception {
        LifePlan otherPlan = insertLifePlan(otherPatientId, "active", "success", "别人方案");

        mockMvc.perform(get("/api/life-plans/" + otherPlan.getPlanId())
                        .header("Authorization", bearerToken(patientId, "patient_lp", RoleConstants.PATIENT)))
                .andExpect(status().isNotFound());
    }

    @Test
    void detailMissingReturns404() throws Exception {
        mockMvc.perform(get("/api/life-plans/99999")
                        .header("Authorization", bearerToken(patientId, "patient_lp", RoleConstants.PATIENT)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanListLifePlans() throws Exception {
        insertLifePlan(patientId, "active", "success", "我的方案");

        mockMvc.perform(get("/api/admin/life-plans")
                        .header("Authorization", bearerToken(adminId, "admin_lp", RoleConstants.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].username").value("patient_lp"));
    }

    @Test
    void patientCannotListAdminLifePlans() throws Exception {
        mockMvc.perform(get("/api/admin/life-plans")
                        .header("Authorization", bearerToken(patientId, "patient_lp", RoleConstants.PATIENT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminFiltersWork() throws Exception {
        insertLifePlan(patientId, "active", "success", "成功方案");
        insertLifePlan(otherPatientId, "history", "failed", "失败方案");

        mockMvc.perform(get("/api/admin/life-plans?keyword=patient_lp&status=active&call_status=success&start_date=2026-01-01&end_date=2026-12-31")
                        .header("Authorization", bearerToken(adminId, "admin_lp", RoleConstants.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].plan_title").value("成功方案"));
    }

    @Test
    void adminCanFilterByPlanIdAndFrontendFailedStatus() throws Exception {
        insertLifePlan(patientId, "active", "success", "success plan");
        LifePlan failedPlan = insertLifePlan(otherPatientId, "history", "failed", "failed plan");

        mockMvc.perform(get("/api/admin/life-plans")
                        .param("plan_id", String.valueOf(failedPlan.getPlanId()))
                        .param("status", "failed")
                        .header("Authorization", bearerToken(adminId, "admin_lp", RoleConstants.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].plan_id").value(failedPlan.getPlanId()))
                .andExpect(jsonPath("$.data.list[0].call_status").value("failed"));
    }

    @Test
    void weightFallsBackToProfileBaseWeight() throws Exception {
        insertProfile(patientId);
        insertMetric(patientId, null);
        insertAssessment(patientId, "medium", "success");
        when(difyService.callLifePlan(any(), eq("user-" + patientId))).thenReturn(successResult());

        mockMvc.perform(postGenerate(patientId)).andExpect(status().isOk());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(difyService).callLifePlan(captor.capture(), eq("user-" + patientId));
        Object rawLatestHealthData = captor.getValue().get("latest_health_data_json");
        assertThat(rawLatestHealthData).isInstanceOf(String.class);
        Map<?, ?> latestHealthData = objectMapper.readValue((String) rawLatestHealthData, Map.class);
        assertThat(latestHealthData.get("weight_kg").toString()).isEqualTo("73.2");
        assertThat(captor.getValue().get("user_profile_json")).isInstanceOf(String.class);
        assertThat(captor.getValue().get("risk_result_json")).isInstanceOf(String.class);
        assertThat(captor.getValue().get("avoid_items")).isInstanceOf(String.class);
        assertThat(captor.getValue().get("plan_days")).isEqualTo("7");
    }

    private Integer insertUser(String username, String phone, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setEmail(username + "@example.com");
        user.setAvatar("/uploads/avatar/default.png");
        user.setPasswordHash(PasswordUtil.hashPassword("123456"));
        user.setRole(role);
        user.setStatus(StatusConstants.ACTIVE);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user.getUserId();
    }

    private void seedGenerationData(Integer userId) {
        insertProfile(userId);
        insertMetric(userId, new BigDecimal("72.5"));
        insertAssessment(userId, "medium", "success");
    }

    private void insertProfile(Integer userId) {
        PatientProfileSnapshot profile = new PatientProfileSnapshot();
        profile.setUserId(userId);
        profile.setAge(45);
        profile.setGender("male");
        profile.setHeightCm(new BigDecimal("172.0"));
        profile.setBaseWeightKg(new BigDecimal("73.2"));
        profile.setBaseWaistCm(new BigDecimal("88.0"));
        profile.setFamilyHistory("父亲糖尿病");
        profile.setChronicHistory("高血压");
        profile.setCreateTime(LocalDateTime.now());
        profile.setUpdateTime(LocalDateTime.now());
        profileMapper.insert(profile);
    }

    private void insertMetric(Integer userId, BigDecimal weight) {
        HealthMetricSnapshot metric = new HealthMetricSnapshot();
        metric.setUserId(userId);
        metric.setWeightKg(weight);
        metric.setWaistCm(new BigDecimal("88.5"));
        metric.setSystolicBp(128);
        metric.setDiastolicBp(82);
        metric.setFastingGlucose(new BigDecimal("6.8"));
        metric.setPostprandialGlucose(new BigDecimal("9.1"));
        metric.setHba1c(new BigDecimal("6.5"));
        metric.setDietStatus("偏油");
        metric.setExerciseStatus("每周2次");
        metric.setRecordedAt(LocalDate.now());
        metric.setCreateTime(LocalDateTime.now());
        metricMapper.insert(metric);
    }

    private void insertAssessment(Integer userId, String riskLevel, String callStatus) {
        RiskAssessmentSnapshot assessment = new RiskAssessmentSnapshot();
        assessment.setUserId(userId);
        assessment.setRequestSummary("risk input");
        assessment.setResponseResult("{\"summary\":\"中风险\"}");
        assessment.setRiskLevel(riskLevel);
        assessment.setRiskScore(62);
        assessment.setCallStatus(callStatus);
        assessment.setCreateTime(LocalDateTime.now());
        assessmentMapper.insert(assessment);
    }

    private LifePlan insertLifePlan(Integer userId, String status, String callStatus, String title) {
        PatientProfileSnapshot profile = findOrInsertProfile(userId);
        HealthMetricSnapshot metric = findOrInsertMetric(userId);
        RiskAssessmentSnapshot assessment = findOrInsertAssessment(userId);
        LifePlan plan = new LifePlan();
        plan.setUserId(userId);
        plan.setProfileId(profile.getProfileId());
        plan.setMetricId(metric.getMetricId());
        plan.setAssessmentId(assessment.getAssessmentId());
        plan.setPlanTitle(title);
        plan.setPlanGoal("控糖");
        plan.setPlanJson("{\"summary\":\"测试方案\",\"diet_plan\":{\"breakfast\":\"燕麦\"},\"checkin_tasks\":[]}");
        plan.setCheckinTasksJson("[]");
        plan.setSummary("测试方案");
        plan.setStatus(status);
        plan.setCallStatus(callStatus);
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        lifePlanMapper.insert(plan);
        return plan;
    }

    private PatientProfileSnapshot findOrInsertProfile(Integer userId) {
        PatientProfileSnapshot profile = profileMapper.selectOne(new LambdaQueryWrapper<PatientProfileSnapshot>()
                .eq(PatientProfileSnapshot::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null) {
            insertProfile(userId);
            profile = profileMapper.selectOne(new LambdaQueryWrapper<PatientProfileSnapshot>()
                    .eq(PatientProfileSnapshot::getUserId, userId)
                    .last("LIMIT 1"));
        }
        return profile;
    }

    private HealthMetricSnapshot findOrInsertMetric(Integer userId) {
        HealthMetricSnapshot metric = metricMapper.selectOne(new LambdaQueryWrapper<HealthMetricSnapshot>()
                .eq(HealthMetricSnapshot::getUserId, userId)
                .last("LIMIT 1"));
        if (metric == null) {
            insertMetric(userId, new BigDecimal("72.5"));
            metric = metricMapper.selectOne(new LambdaQueryWrapper<HealthMetricSnapshot>()
                    .eq(HealthMetricSnapshot::getUserId, userId)
                    .last("LIMIT 1"));
        }
        return metric;
    }

    private RiskAssessmentSnapshot findOrInsertAssessment(Integer userId) {
        RiskAssessmentSnapshot assessment = assessmentMapper.selectOne(new LambdaQueryWrapper<RiskAssessmentSnapshot>()
                .eq(RiskAssessmentSnapshot::getUserId, userId)
                .last("LIMIT 1"));
        if (assessment == null) {
            insertAssessment(userId, "medium", "success");
            assessment = assessmentMapper.selectOne(new LambdaQueryWrapper<RiskAssessmentSnapshot>()
                    .eq(RiskAssessmentSnapshot::getUserId, userId)
                    .last("LIMIT 1"));
        }
        return assessment;
    }

    private LifePlan latestPlan(Integer userId) {
        return lifePlanMapper.selectOne(new LambdaQueryWrapper<LifePlan>()
                .eq(LifePlan::getUserId, userId)
                .orderByDesc(LifePlan::getPlanId)
                .last("LIMIT 1"));
    }

    private DifyWorkflowResult successResult() {
        Map<String, Object> planResult = Map.of(
                "plan_title", "个性化控糖生活方案",
                "diet_plan", Map.of("breakfast", "燕麦粥"),
                "exercise_plan", Map.of("exercise_type", "快走", "frequency", "每周5次"),
                "daily_schedule", List.of(Map.of("time", "07:30", "task", "早餐", "content", "燕麦粥")),
                "checkin_tasks", List.of(Map.of("task_type", "diet", "task_name", "饮食打卡", "description", "记录饮食")),
                "health_tips", List.of("规律作息"),
                "summary", "坚持控糖"
        );
        return new DifyWorkflowResult(Map.of(
                "success", true,
                "input_summary", "Dify summary",
                "plan_result", planResult
        ));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postGenerate(Integer userId) {
        return post("/api/ai/life-plan/generate")
                .header("Authorization", bearerToken(userId, "patient_lp", RoleConstants.PATIENT))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"plan_goal\":\"控糖和减重\",\"avoid_items\":[\"高强度跑步\"],\"plan_days\":7}");
    }

    private String bearerToken(Integer userId, String username, String role) {
        return "Bearer " + jwtUtil.generateToken(userId, username, role);
    }
}
