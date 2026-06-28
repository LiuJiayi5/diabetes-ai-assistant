package com.diabetes.assistant.modules.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.PageUtils;
import com.diabetes.assistant.modules.aichat.contract.AiChatQueryApi;
import com.diabetes.assistant.modules.aichat.contract.dto.AiChatSessionSummaryDTO;
import com.diabetes.assistant.modules.checkin.contract.CheckinQueryApi;
import com.diabetes.assistant.modules.checkin.contract.dto.CheckinAnalysisDTO;
import com.diabetes.assistant.modules.checkin.contract.dto.CheckinRecordDTO;
import com.diabetes.assistant.modules.healthmetric.contract.HealthMetricQueryApi;
import com.diabetes.assistant.modules.healthmetric.contract.dto.HealthMetricDTO;
import com.diabetes.assistant.modules.lifeplan.contract.LifePlanQueryApi;
import com.diabetes.assistant.modules.lifeplan.contract.dto.LifePlanDTO;
import com.diabetes.assistant.modules.profile.contract.PatientProfileQueryApi;
import com.diabetes.assistant.modules.profile.contract.dto.PatientProfileDTO;
import com.diabetes.assistant.modules.report.dto.GenerateReportRequest;
import com.diabetes.assistant.modules.report.dto.HealthReportResponse;
import com.diabetes.assistant.modules.report.entity.HealthReport;
import com.diabetes.assistant.modules.report.mapper.HealthReportMapper;
import com.diabetes.assistant.modules.report.service.HealthReportService;
import com.diabetes.assistant.modules.risk.contract.RiskAssessmentQueryApi;
import com.diabetes.assistant.modules.risk.contract.dto.RiskAssessmentDTO;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import com.diabetes.assistant.modules.user.contract.dto.UserBasicDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HealthReportServiceImpl implements HealthReportService {

    private static final String TYPE_PERSONAL = "personal";
    private static final String TYPE_DOCTOR = "doctor_summary";
    private static final String STATUS_GENERATED = "generated";
    private static final String TRACE_PLACEHOLDER = "{{TRACE_URL}}";
    private static final String DISCLAIMER = "本报告仅用于糖尿病预防和健康管理参考，不构成医学诊断、处方或急救建议。";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter HL7_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    private final HealthReportMapper reportMapper;
    private final UserQueryApi userQueryApi;
    private final PatientProfileQueryApi profileQueryApi;
    private final HealthMetricQueryApi metricQueryApi;
    private final RiskAssessmentQueryApi riskQueryApi;
    private final LifePlanQueryApi lifePlanQueryApi;
    private final CheckinQueryApi checkinQueryApi;
    private final AiChatQueryApi aiChatQueryApi;
    private final ObjectMapper objectMapper;

    @Value("${app.report.public-base-url:http://localhost:5173}")
    private String reportPublicBaseUrl;

    @Override
    public HealthReportResponse generate(Integer userId, GenerateReportRequest request) {
        String reportType = normalizeReportType(request.getReportType());
        int days = request.getDays() == null ? 30 : request.getDays();
        ReportContext context = buildContext(userId, reportType, days);
        String markdown = buildMarkdown(context);

        HealthReport report = new HealthReport();
        report.setUserId(userId);
        report.setReportType(reportType);
        report.setReportTitle(context.getReportTitle());
        report.setReportMarkdown(markdown);
        report.setReportSummary(context.getSummary());
        report.setDataSnapshotJson(toJson(context));
        report.setReportStatus(STATUS_GENERATED);
        reportMapper.insert(report);

        context.setTraceUrl(buildTraceUrl(report.getReportId()));
        report.setReportMarkdown(markdown.replace(TRACE_PLACEHOLDER, context.getTraceUrl()));
        report.setDataSnapshotJson(toJson(context));
        reportMapper.updateById(report);
        return toResponse(reportMapper.selectById(report.getReportId()));
    }

    @Override
    public PageResult<HealthReportResponse> list(Integer userId, Integer page, Integer pageSize) {
        int currentPage = PageUtils.normalizePage(page);
        int size = PageUtils.normalizePageSize(pageSize);
        Page<HealthReport> pageResult = reportMapper.selectPage(new Page<>(currentPage, size),
                new LambdaQueryWrapper<HealthReport>()
                        .eq(HealthReport::getUserId, userId)
                        .orderByDesc(HealthReport::getCreateTime)
                        .orderByDesc(HealthReport::getReportId));
        List<HealthReportResponse> list = pageResult.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return new PageResult<>(list, pageResult.getTotal(), currentPage, size);
    }

    @Override
    public HealthReportResponse getDetail(Integer userId, Integer reportId) {
        return toResponse(requireReport(userId, reportId));
    }

    @Override
    public byte[] exportMarkdown(Integer userId, Integer reportId) {
        HealthReport report = requireReport(userId, reportId);
        return displayMarkdown(report).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportPdf(Integer userId, Integer reportId) {
        HealthReport report = requireReport(userId, reportId);
        ReportContext context = readContext(report);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType0Font font = loadChineseFont(document);
            PdfWriter writer = new PdfWriter(document, font);
            writer.startPage();
            writer.writeTitle(report.getReportTitle());
            writer.writeSmall("报告编号：RPT" + report.getReportId() + "    生成时间：" + formatDateTime(report.getCreateTime()));
            writer.writeSmall("追溯链接：" + buildTraceUrl(report.getReportId()));
            writer.drawQrCode(buildTraceUrl(report.getReportId()));
            writer.writeMarkdown(displayMarkdown(report));
            writer.writeSmall(DISCLAIMER);
            writer.close();
            document.save(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new BusinessException(500, "PDF 报告生成失败：" + exception.getMessage());
        }
    }

    @Override
    public byte[] exportFhir(Integer userId, Integer reportId) {
        HealthReport report = requireReport(userId, reportId);
        ReportContext context = readContext(report);
        Map<String, Object> bundle = buildFhirBundle(report, context);
        return toPrettyJson(bundle).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportHl7(Integer userId, Integer reportId) {
        HealthReport report = requireReport(userId, reportId);
        ReportContext context = readContext(report);
        return buildHl7Message(report, context).getBytes(StandardCharsets.UTF_8);
    }

    private ReportContext buildContext(Integer userId, String reportType, int days) {
        LocalDate endDate = LocalDate.now(CHINA_ZONE);
        LocalDate startDate = endDate.minusDays(Math.max(days - 1, 0));
        ReportContext context = new ReportContext();
        context.setUserId(userId);
        context.setReportType(reportType);
        context.setReportTypeLabel(reportTypeLabel(reportType));
        context.setReportTitle(reportTypeLabel(reportType));
        context.setPeriodDays(days);
        context.setStartDate(startDate);
        context.setEndDate(endDate);
        context.setGeneratedAt(LocalDateTime.now(CHINA_ZONE));
        context.setTraceUrl(TRACE_PLACEHOLDER);

        context.setUser(userQueryApi.getUserBasicById(userId));
        context.setProfile(profileQueryApi.getProfileByUserId(userId));
        context.setLatestMetric(metricQueryApi.getLatestMetricByUserId(userId));
        context.setMetrics(metricQueryApi.listMetricsByUserId(userId, startDate, endDate));
        context.setRiskAssessment(riskQueryApi.getLatestAssessmentByUserId(userId));
        context.setLifePlan(lifePlanQueryApi.getCurrentPlanByUserId(userId));
        context.setCheckinRecords(checkinQueryApi.listRecentCheckins(userId, days));
        context.setCompletionRate(checkinQueryApi.getRecentCompletionRate(userId, days));
        context.setCheckinAnalysis(checkinQueryApi.getLatestAnalysisByUserId(userId));
        context.setRecentAiSessions(aiChatQueryApi.listRecentSessionsByUserId(userId, 5));
        context.setAiChatSummary(aiChatQueryApi.getLatestChatSummaryByUserId(userId));

        applyQuality(context);
        context.setSummary(buildReportSummary(context));
        return context;
    }

    private void applyQuality(ReportContext context) {
        List<String> missing = new ArrayList<>();
        if (context.getProfile() == null) {
            missing.add("健康档案");
        }
        if (context.getLatestMetric() == null) {
            missing.add("近期健康指标");
        }
        if (context.getRiskAssessment() == null) {
            missing.add("糖尿病风险评估");
        }
        if (context.getLifePlan() == null) {
            missing.add("当前生活方案");
        }
        if (context.getCheckinRecords().isEmpty()) {
            missing.add("近期打卡记录");
        }
        if (!StringUtils.hasText(context.getAiChatSummary()) || context.getAiChatSummary().startsWith("No ")) {
            missing.add("AI 医生咨询摘要");
        }
        context.setMissingItems(missing);
    }

    private String buildReportSummary(ReportContext context) {
        String risk = riskLabel(context.getRiskAssessment() == null ? null : context.getRiskAssessment().getRiskLevel());
        String metric = context.getLatestMetric() == null ? "暂无近期健康指标" : context.getLatestMetric().getMetricSummary();
        String completion = context.getCompletionRate() == null ? "暂无打卡完成率" : "近" + context.getPeriodDays() + "天打卡完成率约" + context.getCompletionRate() + "%";
        return risk + "；" + metric + "；" + completion + "。";
    }

    private String buildMarkdown(ReportContext context) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(context.getReportTitle()).append("\n\n");
        md.append("> ").append(DISCLAIMER).append("\n\n");
        md.append("- 报告类型：").append(context.getReportTypeLabel()).append("\n");
        md.append("- 报告版本：v1.0\n");
        md.append("- 报告周期：").append(context.getStartDate()).append(" 至 ").append(context.getEndDate()).append("\n");
        md.append("- 追溯链接：").append(context.getTraceUrl()).append("\n\n");

        if (TYPE_DOCTOR.equals(context.getReportType())) {
            appendDoctorOpening(md, context);
        } else {
            appendPersonalOpening(md, context);
        }

        appendProfile(md, context.getProfile());
        appendMetrics(md, context);
        appendRisk(md, context.getRiskAssessment());
        appendLifePlan(md, context.getLifePlan());
        appendCheckin(md, context);
        appendAiChat(md, context);
        appendDataSources(md, context);

        md.append("## 九、下一步建议\n\n");
        md.append("- 携带本报告及近期线下检查结果，与内分泌科或全科医生沟通。\n");
        md.append("- 若近期空腹血糖多次达到或超过 7.0 mmol/L，或餐后血糖达到或超过 11.1 mmol/L，建议尽快线下复查。\n");
        md.append("- 若出现胸痛、意识异常、严重低血糖、持续呕吐、酮体阳性或足部感染，应及时就医。\n\n");

        md.append("## 十、报告局限性\n\n");
        md.append(DISCLAIMER).append("报告由系统根据用户主动填写的数据、Dify 风险预测结果、生活方案、打卡行为分析和 AI 咨询摘要自动整理，数据缺失或填写不准确会影响报告完整性。\n");
        return md.toString();
    }

    private void appendPersonalOpening(StringBuilder md, ReportContext context) {
        md.append("## 一、我的控糖小结 🌿\n\n");
        md.append("　　这份报告把你最近的健康档案、血糖血压、风险评估、生活方案和打卡情况整理在一起，方便你快速了解自己这一阶段的状态。\n\n");
        md.append("　　当前整体情况是：").append(context.getSummary()).append("如果某几项指标偶尔不理想，也不用被数字吓住，先把记录做稳定，再根据医生建议一点点调整。\n\n");
        md.append("### 本周期执行小仪表盘 📊\n\n");
        md.append("| 项目 | 当前状态 |\n| --- | --- |\n");
        md.append("| 风险提示 | ").append(riskLabel(context.getRiskAssessment() == null ? null : context.getRiskAssessment().getRiskLevel())).append(" |\n");
        md.append("| 打卡完成率 | ").append(context.getCompletionRate() == null ? "暂无" : context.getCompletionRate() + "%").append(" |\n");
        md.append("| 近期记录 | ").append(context.getMetrics().size()).append(" 条健康指标，").append(context.getCheckinRecords().size()).append(" 条打卡 |\n");
        md.append("| 资料提醒 | ").append(context.getMissingItems().isEmpty() ? "资料比较齐，可以继续观察趋势。" : "还缺少" + String.join("、", context.getMissingItems()) + "，补齐后报告会更有参考价值。").append(" |\n\n");
        md.append("　　下个阶段可以先抓三件小事：稳定记录血糖，优先完成饮食和运动打卡，发现连续异常时及时线下复查。把目标拆小一点，更容易坚持下来。✨\n\n");
    }

    private void appendDoctorOpening(StringBuilder md, ReportContext context) {
        md.append("## 一、医生速览\n\n");
        md.append("- 综合摘要：").append(context.getSummary()).append("\n");
        md.append("- 风险等级：").append(riskLabel(context.getRiskAssessment() == null ? null : context.getRiskAssessment().getRiskLevel())).append("\n");
        md.append("- 关注问题：糖尿病风险评估、近期血糖/血压/BMI变化、生活方案执行情况。\n");
        md.append("- 主要缺失：").append(context.getMissingItems().isEmpty() ? "暂无明显缺失" : String.join("、", context.getMissingItems())).append("\n");
        md.append("- 隐私处理：导出文件仅使用系统匿名用户编号，不包含身份证、手机号或住址。\n\n");
    }

    private void appendProfile(StringBuilder md, PatientProfileDTO profile) {
        md.append("## 二、基本健康档案\n\n");
        if (profile == null) {
            md.append("暂无健康档案。\n\n");
            return;
        }
        md.append("- 年龄：").append(value(profile.getAge())).append(" 岁\n");
        md.append("- 性别：").append(value(profile.getGender())).append("\n");
        md.append("- 身高：").append(value(profile.getHeightCm())).append(" cm\n");
        md.append("- 基础体重：").append(value(profile.getBaseWeightKg())).append(" kg\n");
        md.append("- 基础腰围：").append(value(profile.getBaseWaistCm())).append(" cm\n");
        md.append("- 家族史：").append(value(profile.getFamilyHistory())).append("\n");
        md.append("- 慢病史：").append(value(profile.getChronicHistory())).append("\n\n");
    }

    private void appendMetrics(StringBuilder md, ReportContext context) {
        md.append("## 三、近期健康指标\n\n");
        HealthMetricDTO metric = context.getLatestMetric();
        if (metric == null) {
            md.append("暂无近期健康指标。\n\n");
            return;
        }
        md.append("| 指标 | 最近值 |\n| --- | --- |\n");
        md.append("| 记录日期 | ").append(value(metric.getRecordedAt())).append(" |\n");
        md.append("| 体重 | ").append(value(metric.getWeightKg())).append(" kg |\n");
        md.append("| 腰围 | ").append(value(metric.getWaistCm())).append(" cm |\n");
        md.append("| 血压 | ").append(value(metric.getSystolicBp())).append("/").append(value(metric.getDiastolicBp())).append(" mmHg |\n");
        md.append("| 空腹血糖 | ").append(value(metric.getFastingGlucose())).append(" mmol/L |\n");
        md.append("| 餐后血糖 | ").append(value(metric.getPostprandialGlucose())).append(" mmol/L |\n");
        md.append("| HbA1c | ").append(value(metric.getHba1c())).append(" % |\n\n");
        md.append("- 近期记录数：").append(context.getMetrics().size()).append(" 条\n\n");
    }

    private void appendRisk(StringBuilder md, RiskAssessmentDTO risk) {
        md.append("## 四、糖尿病风险评估\n\n");
        if (risk == null) {
            md.append("暂无风险评估结果。\n\n");
            return;
        }
        md.append("- 风险等级：").append(riskLabel(risk.getRiskLevel())).append("\n");
        md.append("- 风险评分：").append(value(risk.getRiskScore())).append("\n");
        md.append("- 类型倾向：").append(value(risk.getDiabetesTypeTendency())).append("\n");
        md.append("- 主要风险因素：").append(value(risk.getMainRiskFactors())).append("\n");
        md.append("- 指标分析：").append(value(risk.getIndicatorAnalysis())).append("\n");
        md.append("- 健康建议：").append(value(risk.getHealthAdvice())).append("\n");
        md.append("- 就医提醒：").append(value(risk.getMedicalWarning())).append("\n\n");
    }

    private void appendLifePlan(StringBuilder md, LifePlanDTO plan) {
        md.append("## 五、当前生活方案\n\n");
        if (plan == null) {
            md.append("暂无当前生活方案。\n\n");
            return;
        }
        md.append("- 方案标题：").append(value(plan.getPlanTitle())).append("\n");
        md.append("- 方案目标：").append(value(plan.getPlanGoal())).append("\n");
        md.append("- 方案摘要：").append(value(plan.getSummary())).append("\n");
        md.append("- 生成时间：").append(formatDateTime(plan.getCreateTime())).append("\n\n");
    }

    private void appendCheckin(StringBuilder md, ReportContext context) {
        md.append("## 六、打卡执行与行为分析\n\n");
        md.append("- 近").append(context.getPeriodDays()).append("天打卡记录数：").append(context.getCheckinRecords().size()).append(" 条\n");
        md.append("- 完成率：").append(context.getCompletionRate() == null ? "暂无" : context.getCompletionRate() + "%").append("\n");
        CheckinAnalysisDTO analysis = context.getCheckinAnalysis();
        if (analysis != null) {
            md.append("- 饮食总结：").append(value(analysis.getDietSummary())).append("\n");
            md.append("- 运动总结：").append(value(analysis.getExerciseSummary())).append("\n");
            md.append("- 主要问题：").append(value(analysis.getMainProblems())).append("\n");
            md.append("- 改进建议：").append(value(analysis.getImprovementSuggestions())).append("\n");
            md.append("- 下阶段重点：").append(value(analysis.getNextFocus())).append("\n");
        }
        md.append("\n");
    }

    private void appendAiChat(StringBuilder md, ReportContext context) {
        md.append("## 七、AI 医生咨询摘要\n\n");
        md.append("- 咨询会话数：").append(context.getRecentAiSessions().size()).append(" 个近期会话\n");
        md.append("- 最近咨询摘要：").append(value(context.getAiChatSummary())).append("\n\n");
    }

    private void appendDataSources(StringBuilder md, ReportContext context) {
        md.append("## 八、数据来源追踪\n\n");
        md.append("| 报告内容 | 系统数据来源 |\n| --- | --- |\n");
        md.append("| 基本信息 | patient_profiles 健康档案 |\n");
        md.append("| 血糖、血压、体重、腰围 | health_metrics 近期健康指标，共 ").append(context.getMetrics().size()).append(" 条 |\n");
        md.append("| 风险等级和风险因素 | risk_assessments 糖尿病风险预测结果 |\n");
        md.append("| 饮食、运动、作息建议 | life_plans 个性化生活方案 |\n");
        md.append("| 执行率和行为问题 | checkin_records / checkin_analysis 打卡与行为分析 |\n");
        md.append("| 咨询摘要 | ai_chat_sessions / ai_chat_messages AI 医生咨询记录 |\n\n");
    }

    private Map<String, Object> buildFhirBundle(HealthReport report, ReportContext context) {
        String timestamp = report.getCreateTime().atZone(CHINA_ZONE).toOffsetDateTime().toString();
        String patientRef = "urn:uuid:patient-" + report.getUserId();
        String compositionRef = "urn:uuid:composition-" + report.getReportId();
        String reportRef = "urn:uuid:diagnostic-report-" + report.getReportId();

        List<Map<String, Object>> entries = new ArrayList<>();
        entries.add(entry(compositionRef, compositionResource(report, context, patientRef, timestamp)));
        entries.add(entry(patientRef, patientResource(context)));
        entries.add(entry("urn:uuid:observation-metric-" + report.getReportId(), observationResource(report, context, patientRef, timestamp)));
        entries.add(entry("urn:uuid:risk-" + report.getReportId(), riskResource(report, context, patientRef, timestamp)));
        entries.add(entry("urn:uuid:careplan-" + report.getReportId(), carePlanResource(report, context, patientRef, timestamp)));
        entries.add(entry(reportRef, diagnosticReportResource(report, context, patientRef, timestamp)));

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("resourceType", "Bundle");
        bundle.put("type", "document");
        bundle.put("identifier", Map.of("system", "urn:diabetes-ai-assistant:report", "value", "report-" + report.getReportId()));
        bundle.put("timestamp", timestamp);
        bundle.put("entry", entries);
        return bundle;
    }

    private Map<String, Object> compositionResource(HealthReport report, ReportContext context, String patientRef, String timestamp) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("resourceType", "Composition");
        resource.put("id", "composition-" + report.getReportId());
        resource.put("status", "final");
        resource.put("type", codeable("DMREPORT", "糖尿病健康管理报告"));
        resource.put("subject", Map.of("reference", patientRef));
        resource.put("date", timestamp);
        resource.put("title", report.getReportTitle());
        resource.put("section", List.of(
                section("近期健康指标", "Observation/metric-" + report.getReportId()),
                section("风险评估", "RiskAssessment/risk-" + report.getReportId()),
                section("生活干预计划", "CarePlan/careplan-" + report.getReportId()),
                section("报告摘要", "DiagnosticReport/diagnostic-report-" + report.getReportId())
        ));
        return resource;
    }

    private Map<String, Object> patientResource(ReportContext context) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("resourceType", "Patient");
        resource.put("id", "patient-" + context.getUserId());
        resource.put("identifier", List.of(Map.of("system", "urn:diabetes-ai-assistant:user", "value", "U" + context.getUserId())));
        resource.put("name", List.of(Map.of("text", "Anonymous")));
        PatientProfileDTO profile = context.getProfile();
        if (profile != null) {
            resource.put("gender", mapGender(profile.getGender()));
        }
        return resource;
    }

    private Map<String, Object> observationResource(HealthReport report, ReportContext context, String patientRef, String timestamp) {
        HealthMetricDTO metric = context.getLatestMetric();
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("resourceType", "Observation");
        resource.put("id", "metric-" + report.getReportId());
        resource.put("status", "final");
        resource.put("code", codeable("HEALTH_METRICS", "近期健康指标"));
        resource.put("subject", Map.of("reference", patientRef));
        resource.put("effectiveDateTime", metric == null || metric.getRecordedAt() == null ? timestamp : metric.getRecordedAt().toString());
        resource.put("component", List.of(
                component("FASTING_GLUCOSE", "空腹血糖", metric == null ? null : metric.getFastingGlucose(), "mmol/L"),
                component("POSTPRANDIAL_GLUCOSE", "餐后血糖", metric == null ? null : metric.getPostprandialGlucose(), "mmol/L"),
                component("HBA1C", "糖化血红蛋白", metric == null ? null : metric.getHba1c(), "%"),
                component("WEIGHT", "体重", metric == null ? null : metric.getWeightKg(), "kg"),
                component("WAIST", "腰围", metric == null ? null : metric.getWaistCm(), "cm")
        ));
        return resource;
    }

    private Map<String, Object> riskResource(HealthReport report, ReportContext context, String patientRef, String timestamp) {
        RiskAssessmentDTO risk = context.getRiskAssessment();
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("resourceType", "RiskAssessment");
        resource.put("id", "risk-" + report.getReportId());
        resource.put("status", "final");
        resource.put("subject", Map.of("reference", patientRef));
        resource.put("occurrenceDateTime", timestamp);
        resource.put("prediction", List.of(Map.of(
                "outcome", codeable("DIABETES_RISK", riskLabel(risk == null ? null : risk.getRiskLevel())),
                "probabilityDecimal", riskProbability(risk),
                "rationale", value(risk == null ? null : risk.getMainRiskFactors())
        )));
        resource.put("note", List.of(Map.of("text", value(risk == null ? null : risk.getSummary()))));
        return resource;
    }

    private Map<String, Object> carePlanResource(HealthReport report, ReportContext context, String patientRef, String timestamp) {
        LifePlanDTO plan = context.getLifePlan();
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("resourceType", "CarePlan");
        resource.put("id", "careplan-" + report.getReportId());
        resource.put("status", "active");
        resource.put("intent", "plan");
        resource.put("subject", Map.of("reference", patientRef));
        resource.put("created", timestamp);
        resource.put("title", plan == null ? "糖尿病健康管理生活方案" : plan.getPlanTitle());
        resource.put("description", plan == null ? "暂无当前生活方案" : plan.getSummary());
        resource.put("note", List.of(Map.of("text", "目标：" + value(plan == null ? null : plan.getPlanGoal()))));
        return resource;
    }

    private Map<String, Object> diagnosticReportResource(HealthReport report, ReportContext context, String patientRef, String timestamp) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("resourceType", "DiagnosticReport");
        resource.put("id", "diagnostic-report-" + report.getReportId());
        resource.put("status", "final");
        resource.put("code", codeable("DMREPORT", report.getReportTitle()));
        resource.put("subject", Map.of("reference", patientRef));
        resource.put("effectiveDateTime", timestamp);
        resource.put("conclusion", report.getReportSummary());
        resource.put("presentedForm", List.of(Map.of(
                "contentType", "text/markdown; charset=UTF-8",
                "data", Base64.getEncoder().encodeToString(displayMarkdown(report).getBytes(StandardCharsets.UTF_8)),
                "title", report.getReportTitle() + ".md"
        )));
        return resource;
    }

    private String buildHl7Message(HealthReport report, ReportContext context) {
        String time = report.getCreateTime().format(HL7_TIME);
        List<String> segments = new ArrayList<>();
        segments.add("MSH|^~\\&|DiabetesAssistant|TrainingProject|ExternalSystem|Clinic|" + time + "||MDM^T02|MSG" + report.getReportId() + "|P|2.5.1||||||UNICODE UTF-8");
        segments.add("EVN|T02|" + time);
        segments.add("PID|1||U" + report.getUserId() + "^^^DiabetesAssistant^MR||Anonymous");
        segments.add("PV1|1|O");
        segments.add("TXA|1|DMREPORT|TX|" + time + "|||||||Diabetes AI Assistant||RPT" + report.getReportId() + "||||" + escapeHl7(report.getReportTitle()) + "|AU|N");
        segments.add(obx(1, "REPORT_TYPE", "报告类型", context.getReportTypeLabel()));
        segments.add(obx(2, "RISK_LEVEL", "风险等级", riskLabel(context.getRiskAssessment() == null ? null : context.getRiskAssessment().getRiskLevel())));
        segments.add(obx(3, "FASTING_GLUCOSE", "空腹血糖", metricValue(context.getLatestMetric(), "fasting")));
        segments.add(obx(4, "POSTPRANDIAL_GLUCOSE", "餐后血糖", metricValue(context.getLatestMetric(), "postprandial")));
        segments.add(obx(5, "HBA1C", "糖化血红蛋白", metricValue(context.getLatestMetric(), "hba1c")));
        segments.add(obx(6, "BLOOD_PRESSURE", "血压", metricValue(context.getLatestMetric(), "bp")));
        segments.add(obx(7, "COMPLETION_RATE", "打卡完成率", context.getCompletionRate() == null ? "暂无" : context.getCompletionRate() + "%"));
        segments.add(obx(8, "SUMMARY", "报告摘要", report.getReportSummary()));
        segments.add(obx(9, "RECOMMENDATION", "建议", "建议携带本报告咨询内分泌科医生，并结合线下检查确认。"));
        return String.join("\r", segments) + "\r";
    }

    private HealthReport requireReport(Integer userId, Integer reportId) {
        HealthReport report = reportMapper.selectById(reportId);
        if (report == null || !report.getUserId().equals(userId)) {
            throw new BusinessException(404, "报告不存在");
        }
        return report;
    }

    private HealthReportResponse toResponse(HealthReport report) {
        HealthReportResponse response = new HealthReportResponse();
        response.setReportId(report.getReportId());
        response.setReportType(report.getReportType());
        response.setReportTypeLabel(reportTypeLabel(report.getReportType()));
        response.setReportTitle(report.getReportTitle());
        response.setReportSummary(report.getReportSummary());
        response.setReportMarkdown(displayMarkdown(report));
        response.setReportStatus(report.getReportStatus());
        response.setTraceUrl(buildTraceUrl(report.getReportId()));
        response.setQrCodeDataUrl(buildQrCodeDataUrl(buildTraceUrl(report.getReportId())));
        response.setCreateTime(report.getCreateTime());
        response.setUpdateTime(report.getUpdateTime());
        ReportContext context = readContext(report);
        response.setCompletionRate(context.getCompletionRate());
        response.setRiskLevelLabel(riskLabel(context.getRiskAssessment() == null ? null : context.getRiskAssessment().getRiskLevel()));
        response.setMissingItems(context.getMissingItems());
        return response;
    }

    private ReportContext readContext(HealthReport report) {
        if (!StringUtils.hasText(report.getDataSnapshotJson())) {
            ReportContext fallback = new ReportContext();
            fallback.setUserId(report.getUserId());
            fallback.setReportType(report.getReportType());
            fallback.setReportTypeLabel(reportTypeLabel(report.getReportType()));
            fallback.setReportTitle(report.getReportTitle());
            fallback.setSummary(report.getReportSummary());
            fallback.setMissingItems(List.of());
            return fallback;
        }
        try {
            ReportContext context = objectMapper.readValue(report.getDataSnapshotJson(), ReportContext.class);
            context.setTraceUrl(buildTraceUrl(report.getReportId()));
            return context;
        } catch (Exception exception) {
            throw new BusinessException(500, "报告快照读取失败");
        }
    }

    private String displayMarkdown(HealthReport report) {
        return stripCompletenessScore(report.getReportMarkdown());
    }

    private String stripCompletenessScore(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }
        return markdown.replaceAll("(?m)^- 完整度评分：[^\\r\\n]*(\\r?\\n)?", "");
    }

    private String normalizeReportType(String reportType) {
        if (TYPE_DOCTOR.equals(reportType)) {
            return TYPE_DOCTOR;
        }
        return TYPE_PERSONAL;
    }

    private String reportTypeLabel(String reportType) {
        return TYPE_DOCTOR.equals(reportType) ? "糖尿病就医沟通摘要" : "糖尿病健康管理个人报告";
    }

    private String riskLabel(String riskLevel) {
        if (!StringUtils.hasText(riskLevel)) {
            return "暂无评估";
        }
        String normalized = riskLevel.toLowerCase();
        if (normalized.contains("high") || riskLevel.contains("高")) {
            return "高风险";
        }
        if (normalized.contains("low") || riskLevel.contains("低")) {
            return "低风险";
        }
        return "中风险";
    }

    private String value(Object value) {
        if (value == null) {
            return "暂无";
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "暂无" : text;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "暂无" : value.format(DATE_TIME);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "报告快照生成失败");
        }
    }

    private String toPrettyJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "FHIR 导出生成失败");
        }
    }

    private String buildTraceUrl(Integer reportId) {
        String baseUrl = StringUtils.hasText(reportPublicBaseUrl) ? reportPublicBaseUrl.trim() : "http://localhost:5173";
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/app/reports/" + reportId;
    }

    private String buildQrCodeDataUrl(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 180, 180);
            BufferedImage qr = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(qr, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            return null;
        }
    }

    private Map<String, Object> entry(String fullUrl, Map<String, Object> resource) {
        return Map.of("fullUrl", fullUrl, "resource", resource);
    }

    private Map<String, Object> codeable(String code, String display) {
        return Map.of("coding", List.of(Map.of("system", "urn:diabetes-ai-assistant:code", "code", code, "display", display)), "text", display);
    }

    private Map<String, Object> section(String title, String reference) {
        return Map.of("title", title, "entry", List.of(Map.of("reference", reference)));
    }

    private Map<String, Object> component(String code, String display, BigDecimal value, String unit) {
        Map<String, Object> component = new LinkedHashMap<>();
        component.put("code", codeable(code, display));
        if (value != null) {
            component.put("valueQuantity", Map.of("value", value, "unit", unit, "system", "urn:unit", "code", unit));
        }
        return component;
    }

    private String mapGender(String gender) {
        if (!StringUtils.hasText(gender)) {
            return "unknown";
        }
        if (gender.contains("男") || "male".equalsIgnoreCase(gender)) {
            return "male";
        }
        if (gender.contains("女") || "female".equalsIgnoreCase(gender)) {
            return "female";
        }
        return "unknown";
    }

    private String metricValue(HealthMetricDTO metric, String key) {
        if (metric == null) {
            return "暂无";
        }
        if ("fasting".equals(key)) {
            return value(metric.getFastingGlucose()) + " mmol/L";
        }
        if ("postprandial".equals(key)) {
            return value(metric.getPostprandialGlucose()) + " mmol/L";
        }
        if ("hba1c".equals(key)) {
            return value(metric.getHba1c()) + "%";
        }
        if ("bp".equals(key)) {
            return value(metric.getSystolicBp()) + "/" + value(metric.getDiastolicBp()) + " mmHg";
        }
        return "暂无";
    }

    private BigDecimal riskProbability(RiskAssessmentDTO risk) {
        if (risk == null || risk.getRiskScore() == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(risk.getRiskScore())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String obx(int index, String code, String label, String value) {
        return "OBX|" + index + "|TX|" + code + "^" + escapeHl7(label) + "^L||" + escapeHl7(value) + "||||||F";
    }

    private String escapeHl7(String value) {
        return value(value)
                .replace("\\", "\\E\\")
                .replace("|", "\\F\\")
                .replace("^", "\\S\\")
                .replace("&", "\\T\\")
                .replace("~", "\\R\\")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private PDType0Font loadChineseFont(PDDocument document) throws IOException {
        List<String> candidates = List.of(
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/simsun.ttc"
        );
        for (String path : candidates) {
            try {
                return PDType0Font.load(document, new java.io.File(path));
            } catch (Exception ignored) {
                // Try the next local Windows font.
            }
        }
        throw new IOException("缺少可用中文字体");
    }

    @Data
    public static class ReportContext {
        private Integer userId;
        private String reportType;
        private String reportTypeLabel;
        private String reportTitle;
        private Integer periodDays;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime generatedAt;
        private UserBasicDTO user;
        private PatientProfileDTO profile;
        private HealthMetricDTO latestMetric;
        private List<HealthMetricDTO> metrics = List.of();
        private RiskAssessmentDTO riskAssessment;
        private LifePlanDTO lifePlan;
        private List<CheckinRecordDTO> checkinRecords = List.of();
        private BigDecimal completionRate;
        private CheckinAnalysisDTO checkinAnalysis;
        private List<AiChatSessionSummaryDTO> recentAiSessions = List.of();
        private String aiChatSummary;
        private List<String> missingItems = List.of();
        private String summary;
        private String traceUrl;
    }

    private class PdfWriter {
        private static final float MARGIN = 48;
        private static final float FONT_SIZE = 11;
        private static final float LINE_HEIGHT = 18;

        private final PDDocument document;
        private final PDType0Font font;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        PdfWriter(PDDocument document, PDType0Font font) {
            this.document = document;
            this.font = font;
        }

        void startPage() throws IOException {
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        void writeTitle(String text) throws IOException {
            writeLine(text, 18, 1.4f);
        }

        void writeSmall(String text) throws IOException {
            writeLine(text, 9, 1.2f);
        }

        void writeMarkdown(String markdown) throws IOException {
            for (String rawLine : markdown.split("\\R")) {
                String line = rawLine.replace("#", "").replace(">", "").trim();
                if (!StringUtils.hasText(line)) {
                    y -= 8;
                    continue;
                }
                float size = rawLine.startsWith("#") ? 14 : FONT_SIZE;
                for (String wrapped : wrap(line, size, 86)) {
                    writeLine(wrapped, size, 1.0f);
                }
            }
        }

        void drawQrCode(String text) throws Exception {
            BufferedImage qr = createQr(text);
            ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
            ImageIO.write(qr, "png", imageOut);
            PDImageXObject image = PDImageXObject.createFromByteArray(document, imageOut.toByteArray(), "report-qr");
            float size = 72;
            stream.drawImage(image, page.getMediaBox().getWidth() - MARGIN - size, page.getMediaBox().getHeight() - MARGIN - size, size, size);
        }

        private void writeLine(String text, float size, float spacingMultiplier) throws IOException {
            ensureSpace(LINE_HEIGHT * spacingMultiplier);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(text);
            stream.endText();
            y -= LINE_HEIGHT * spacingMultiplier;
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed > MARGIN) {
                return;
            }
            stream.close();
            startPage();
        }

        private List<String> wrap(String text, float size, int maxChars) {
            List<String> lines = new ArrayList<>();
            String remaining = text;
            while (remaining.length() > maxChars) {
                lines.add(remaining.substring(0, maxChars));
                remaining = remaining.substring(maxChars);
            }
            lines.add(remaining);
            return lines;
        }

        private void close() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        private BufferedImage createQr(String text) throws Exception {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 180, 180);
            return MatrixToImageWriter.toBufferedImage(matrix);
        }
    }
}
