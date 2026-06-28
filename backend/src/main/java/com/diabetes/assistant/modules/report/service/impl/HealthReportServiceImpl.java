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
import com.diabetes.assistant.modules.dify.dto.DifyWorkflowResult;
import com.diabetes.assistant.modules.dify.service.DifyService;
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
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.Comparator;
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
    private final DifyService difyService;
    private final ObjectMapper objectMapper;

    @Value("${app.report.public-base-url:http://localhost:5173}")
    private String reportPublicBaseUrl;

    @Override
    public HealthReportResponse generate(Integer userId, GenerateReportRequest request) {
        String reportType = normalizeReportType(request.getReportType());
        int days = request.getDays() == null ? 30 : request.getDays();
        ReportContext context = buildContext(userId, reportType, days);
        enrichReportIntelligence(context);
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
    public HealthReportResponse getPublicDetail(Integer reportId) {
        HealthReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(404, "报告不存在");
        }
        return toResponse(report);
    }

    @Override
    public byte[] exportPublicHtml(Integer reportId) {
        HealthReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(404, "报告不存在");
        }
        return buildPublicHtml(report).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportPublicPdf(Integer reportId) {
        HealthReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(404, "报告不存在");
        }
        return buildPdf(report);
    }

    @Override
    public byte[] exportMarkdown(Integer userId, Integer reportId) {
        HealthReport report = requireReport(userId, reportId);
        return displayMarkdown(report).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportPdf(Integer userId, Integer reportId) {
        HealthReport report = requireReport(userId, reportId);
        return buildPdf(report);
    }

    private byte[] buildPdf(HealthReport report) {
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

    @SuppressWarnings("unchecked")
    private void enrichReportIntelligence(ReportContext context) {
        try {
            Map<String, Object> inputs = buildComprehensiveReportInputs(context);
            DifyWorkflowResult result = difyService.callComprehensiveReport(inputs, "report-" + context.getUserId());
            ReportIntelligence intelligence = parseReportIntelligence(result == null ? null : result.getOutputs());
            intelligence.setGenerationMode("dify");
            context.setIntelligence(intelligence);
        } catch (Exception exception) {
            ReportIntelligence fallback = buildLocalReportIntelligence(context);
            fallback.setGenerationMode("local-fallback");
            context.setIntelligence(fallback);
        }
        if (context.getIntelligence() != null && StringUtils.hasText(context.getIntelligence().getSummary())) {
            context.setSummary(context.getIntelligence().getSummary());
        }
    }

    private Map<String, Object> buildComprehensiveReportInputs(ReportContext context) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("user_id", String.valueOf(context.getUserId()));
        inputs.put("report_type", context.getReportType());
        inputs.put("period_days", context.getPeriodDays());
        inputs.put("user_profile_json", toJsonOrEmpty(context.getProfile()));
        inputs.put("latest_metrics_json", toJsonOrEmpty(context.getLatestMetric()));
        inputs.put("metrics_json", toJsonOrEmpty(context.getMetrics()));
        inputs.put("risk_assessment_json", toJsonOrEmpty(context.getRiskAssessment()));
        inputs.put("life_plan_json", toJsonOrEmpty(context.getLifePlan()));
        inputs.put("checkin_analysis_json", toJsonOrEmpty(context.getCheckinAnalysis()));
        inputs.put("checkin_records_json", toJsonOrEmpty(context.getCheckinRecords()));
        inputs.put("ai_chat_summary", value(context.getAiChatSummary()));
        inputs.put("missing_items", String.join("、", context.getMissingItems()));
        inputs.put("safety_rules", "仅用于糖尿病健康管理和就医沟通准备，不输出诊断、处方、药物剂量或替代线下就医的结论。");
        return inputs;
    }

    private ReportIntelligence parseReportIntelligence(Map<String, Object> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            throw new BusinessException(502, "Dify comprehensive report response is empty");
        }
        Object success = outputs.get("success");
        if (success != null && !"true".equalsIgnoreCase(String.valueOf(success))) {
            throw new BusinessException(502, "Dify comprehensive report returned success=false");
        }
        Object raw = firstPresent(outputs, "report_result", "report_json", "result", "text", "answer");
        if (raw == null) {
            raw = outputs;
        }
        String json = raw instanceof String text ? stripJsonFence(text) : toJsonOrEmpty(raw);
        try {
            ReportIntelligence intelligence = objectMapper.readValue(json, ReportIntelligence.class);
            if (intelligence.getPatientReport() == null || intelligence.getDoctorReport() == null) {
                throw new BusinessException(502, "Dify comprehensive report is missing required sections");
            }
            normalizeIntelligence(intelligence);
            return intelligence;
        } catch (Exception exception) {
            throw new BusinessException(502, "Dify comprehensive report JSON parse failed");
        }
    }

    private ReportIntelligence buildLocalReportIntelligence(ReportContext context) {
        ReportIntelligence intelligence = new ReportIntelligence();
        PatientReport patient = new PatientReport();
        DoctorReport doctor = new DoctorReport();
        NextCycleAdjustment adjustment = new NextCycleAdjustment();

        String risk = riskLabel(context.getRiskAssessment() == null ? null : context.getRiskAssessment().getRiskLevel());
        String completion = context.getCompletionRate() == null ? "暂无打卡完成率" : "近" + context.getPeriodDays() + "天打卡完成率约 " + context.getCompletionRate() + "%";
        MetricTrendSummary trend = buildMetricTrend(context);

        patient.setFriendlySummary("这份报告把你的健康档案、近期指标、风险评估、生活方案、打卡执行和 AI 咨询摘要合在一起看。当前重点是：" + risk + "，" + completion + "，" + trend.getPlainSummary());
        patient.setKeyFindings(compactList(List.of(
                risk + "，需要结合近期血糖、血压、BMI 和家族史一起理解。",
                trend.getPlainSummary(),
                context.getCheckinAnalysis() == null ? "暂未形成稳定的打卡分析，建议先把饮食、运动和血糖记录补起来。" : value(context.getCheckinAnalysis().getSummary()),
                context.getLifePlan() == null ? "暂无当前生活方案，可先生成一版保守的控糖计划。" : "当前生活方案目标：" + value(context.getLifePlan().getPlanGoal())
        )));
        patient.setTodayAction(buildPatientActions(context));
        patient.setEncouragement("先把能坚持的小动作做好：记录、少量调整、定期复查。只要数据逐渐稳定，下一轮方案就可以更贴近你的真实节奏。");

        doctor.setChiefConcern("糖尿病风险管理与近期生活干预执行情况复盘");
        doctor.setRiskSummary(context.getRiskAssessment() == null ? "暂无风险预测结果。" : value(context.getRiskAssessment().getSummary()) + "；主要风险因素：" + value(context.getRiskAssessment().getMainRiskFactors()));
        doctor.setAbnormalIndicators(buildAbnormalIndicators(context.getLatestMetric()));
        doctor.setBehaviorAdherence(completion + "；" + (context.getCheckinAnalysis() == null ? "暂无最新行为分析。" : value(context.getCheckinAnalysis().getLifeEvaluation())));
        doctor.setClinicalCommunicationPoints(compactList(List.of(
                "请结合线下空腹血糖、餐后血糖、HbA1c、血压和体重变化判断风险。",
                "若用户近期血糖持续异常，建议确认是否需要进一步筛查或复查。",
                "本系统报告来源于用户自填数据和 AI 工作流结果，只作为就医沟通材料。"
        )));
        doctor.setFollowUpSuggestions(compactList(List.of(
                "携带本报告和近期化验结果咨询内分泌科或全科医生。",
                "建议补齐 HbA1c、空腹血糖、餐后血糖、血压和体重腰围记录。",
                "高风险或连续异常时，不建议只依赖线上建议，应进行线下复查。"
        )));

        adjustment.setDietFocus(buildDietFocus(context));
        adjustment.setExerciseFocus(buildExerciseFocus(context));
        adjustment.setMonitoringFocus(buildMonitoringFocus(context));

        intelligence.setPatientReport(patient);
        intelligence.setDoctorReport(doctor);
        intelligence.setEvidenceChain(buildEvidenceChain(context));
        intelligence.setDataGaps(context.getMissingItems().isEmpty() ? List.of("暂无明显核心数据缺口，建议继续保持连续记录。") : context.getMissingItems());
        intelligence.setSafetyWarnings(buildSafetyWarnings(context));
        intelligence.setNextCycleAdjustment(adjustment);
        intelligence.setMetricTrend(trend);
        intelligence.setSummary(risk + "；" + completion + "；" + trend.getPlainSummary());
        normalizeIntelligence(intelligence);
        return intelligence;
    }

    private void normalizeIntelligence(ReportIntelligence intelligence) {
        if (intelligence.getPatientReport() == null) {
            intelligence.setPatientReport(new PatientReport());
        }
        if (intelligence.getDoctorReport() == null) {
            intelligence.setDoctorReport(new DoctorReport());
        }
        if (intelligence.getNextCycleAdjustment() == null) {
            intelligence.setNextCycleAdjustment(new NextCycleAdjustment());
        }
        if (intelligence.getMetricTrend() == null) {
            intelligence.setMetricTrend(new MetricTrendSummary());
        }
        intelligence.setEvidenceChain(safeList(intelligence.getEvidenceChain()));
        intelligence.setDataGaps(safeList(intelligence.getDataGaps()));
        intelligence.setSafetyWarnings(safeList(intelligence.getSafetyWarnings()));
        if (!StringUtils.hasText(intelligence.getSummary())) {
            intelligence.setSummary(value(intelligence.getPatientReport().getFriendlySummary()));
        }
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
        ReportIntelligence intelligence = context.getIntelligence() == null ? buildLocalReportIntelligence(context) : context.getIntelligence();
        md.append("# ").append(context.getReportTitle()).append("\n\n");
        md.append("> ").append(DISCLAIMER).append("\n\n");
        md.append("- 报告类型：").append(context.getReportTypeLabel()).append("\n");
        md.append("- 报告版本：v2.0 综合报告链\n");
        md.append("- 报告周期：").append(context.getStartDate()).append(" 至 ").append(context.getEndDate()).append("\n");
        md.append("- 生成模式：").append("dify".equals(intelligence.getGenerationMode()) ? "Dify 综合报告工作流" : "本地增强规则兜底").append("\n");
        md.append("- 追溯链接：").append(context.getTraceUrl()).append("\n\n");

        if (TYPE_DOCTOR.equals(context.getReportType())) {
            appendDoctorOpening(md, context, intelligence);
        } else {
            appendPersonalOpening(md, context, intelligence);
        }

        appendProfile(md, context.getProfile());
        appendMetrics(md, context);
        appendRisk(md, context.getRiskAssessment());
        appendLifePlan(md, context.getLifePlan());
        appendCheckin(md, context);
        appendAiChat(md, context);
        appendNextCycleAdjustment(md, intelligence);
        appendEvidenceChain(md, intelligence);
        appendDataSources(md, context);

        md.append("## 十一、安全提醒\n\n");
        for (String warning : intelligence.getSafetyWarnings()) {
            md.append("- ").append(warning).append("\n");
        }
        md.append("- 携带本报告及近期线下检查结果，与内分泌科或全科医生沟通。\n");
        md.append("- 若近期空腹血糖多次达到或超过 7.0 mmol/L，或餐后血糖达到或超过 11.1 mmol/L，建议尽快线下复查。\n");
        md.append("- 若出现胸痛、意识异常、严重低血糖、持续呕吐、酮体阳性或足部感染，应及时就医。\n\n");

        md.append("## 十二、数据完整度与局限性\n\n");
        if (intelligence.getDataGaps().isEmpty()) {
            md.append("- 暂无明显核心数据缺口。\n");
        } else {
            for (String gap : intelligence.getDataGaps()) {
                md.append("- ").append(gap).append("\n");
            }
        }
        md.append(DISCLAIMER).append("报告由系统根据用户主动填写的数据、Dify 风险预测结果、生活方案、打卡行为分析和 AI 咨询摘要自动整理，数据缺失或填写不准确会影响报告完整性。\n");
        return md.toString();
    }

    private void appendPersonalOpening(StringBuilder md, ReportContext context, ReportIntelligence intelligence) {
        PatientReport patient = intelligence.getPatientReport();
        md.append("## 一、我的控糖小结 🌿\n\n");
        md.append("　　").append(value(patient.getFriendlySummary())).append("\n\n");
        md.append("　　如果某几项指标偶尔不理想，也不用被数字吓住。先把记录做稳定，再根据医生建议一点点调整，报告会随着你的执行情况越来越贴近真实状态。\n\n");
        md.append("### 本周期执行小仪表盘 📊\n\n");
        md.append("| 项目 | 当前状态 |\n| --- | --- |\n");
        md.append("| 风险提示 | ").append(riskLabel(context.getRiskAssessment() == null ? null : context.getRiskAssessment().getRiskLevel())).append(" |\n");
        md.append("| 打卡完成率 | ").append(context.getCompletionRate() == null ? "暂无" : context.getCompletionRate() + "%").append(" |\n");
        md.append("| 近期记录 | ").append(context.getMetrics().size()).append(" 条健康指标，").append(context.getCheckinRecords().size()).append(" 条打卡 |\n");
        md.append("| 资料提醒 | ").append(context.getMissingItems().isEmpty() ? "资料比较齐，可以继续观察趋势。" : "还缺少" + String.join("、", context.getMissingItems()) + "，补齐后报告会更有参考价值。").append(" |\n\n");
        md.append("### 这次报告最想提醒你的事 ✨\n\n");
        for (String finding : safeList(patient.getKeyFindings())) {
            md.append("- ").append(finding).append("\n");
        }
        md.append("\n### 今天就能先做的小行动\n\n");
        for (String action : safeList(patient.getTodayAction())) {
            md.append("- ").append(action).append("\n");
        }
        md.append("\n　　").append(value(patient.getEncouragement())).append("\n\n");
    }

    private void appendDoctorOpening(StringBuilder md, ReportContext context, ReportIntelligence intelligence) {
        DoctorReport doctor = intelligence.getDoctorReport();
        md.append("## 一、医生速览\n\n");
        md.append("- 主诉/关注问题：").append(value(doctor.getChiefConcern())).append("\n");
        md.append("- 综合摘要：").append(context.getSummary()).append("\n");
        md.append("- 风险等级：").append(riskLabel(context.getRiskAssessment() == null ? null : context.getRiskAssessment().getRiskLevel())).append("\n");
        md.append("- 风险解释：").append(value(doctor.getRiskSummary())).append("\n");
        md.append("- 行为依从性：").append(value(doctor.getBehaviorAdherence())).append("\n");
        md.append("- 主要缺失：").append(context.getMissingItems().isEmpty() ? "暂无明显缺失" : String.join("、", context.getMissingItems())).append("\n");
        md.append("- 隐私处理：导出文件仅使用系统匿名用户编号，不包含身份证、手机号或住址。\n\n");
        md.append("### 异常指标速览\n\n");
        for (String item : safeList(doctor.getAbnormalIndicators())) {
            md.append("- ").append(item).append("\n");
        }
        md.append("\n### 面诊沟通要点\n\n");
        for (String point : safeList(doctor.getClinicalCommunicationPoints())) {
            md.append("- ").append(point).append("\n");
        }
        md.append("\n### 建议复查与随访\n\n");
        for (String suggestion : safeList(doctor.getFollowUpSuggestions())) {
            md.append("- ").append(suggestion).append("\n");
        }
        md.append("\n");
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
        MetricTrendSummary trend = context.getIntelligence() == null ? buildMetricTrend(context) : context.getIntelligence().getMetricTrend();
        md.append("| 指标 | 最近值 |\n| --- | --- |\n");
        md.append("| 记录日期 | ").append(value(metric.getRecordedAt())).append(" |\n");
        md.append("| 体重 | ").append(value(metric.getWeightKg())).append(" kg |\n");
        md.append("| 腰围 | ").append(value(metric.getWaistCm())).append(" cm |\n");
        md.append("| 血压 | ").append(value(metric.getSystolicBp())).append("/").append(value(metric.getDiastolicBp())).append(" mmHg |\n");
        md.append("| 空腹血糖 | ").append(value(metric.getFastingGlucose())).append(" mmol/L |\n");
        md.append("| 餐后血糖 | ").append(value(metric.getPostprandialGlucose())).append(" mmol/L |\n");
        md.append("| HbA1c | ").append(value(metric.getHba1c())).append(" % |\n\n");
        md.append("- 近期记录数：").append(context.getMetrics().size()).append(" 条\n\n");
        md.append("### 趋势观察\n\n");
        md.append("- ").append(value(trend.getPlainSummary())).append("\n");
        md.append("- 空腹血糖趋势：").append(value(trend.getFastingGlucoseTrend())).append("\n");
        md.append("- 体重趋势：").append(value(trend.getWeightTrend())).append("\n");
        md.append("- 血压趋势：").append(value(trend.getBloodPressureTrend())).append("\n\n");
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
        md.append("## 十、数据来源追踪\n\n");
        md.append("| 报告内容 | 系统数据来源 |\n| --- | --- |\n");
        md.append("| 基本信息 | patient_profiles 健康档案 |\n");
        md.append("| 血糖、血压、体重、腰围 | health_metrics 近期健康指标，共 ").append(context.getMetrics().size()).append(" 条 |\n");
        md.append("| 风险等级和风险因素 | risk_assessments 糖尿病风险预测结果 |\n");
        md.append("| 饮食、运动、作息建议 | life_plans 个性化生活方案 |\n");
        md.append("| 执行率和行为问题 | checkin_records / checkin_analysis 打卡与行为分析 |\n");
        md.append("| 咨询摘要 | ai_chat_sessions / ai_chat_messages AI 医生咨询记录 |\n\n");
    }

    private void appendNextCycleAdjustment(StringBuilder md, ReportIntelligence intelligence) {
        NextCycleAdjustment adjustment = intelligence.getNextCycleAdjustment();
        md.append("## 八、下一周期干预调整\n\n");
        md.append("| 调整方向 | 建议 |\n| --- | --- |\n");
        md.append("| 饮食重点 | ").append(value(adjustment.getDietFocus())).append(" |\n");
        md.append("| 运动重点 | ").append(value(adjustment.getExerciseFocus())).append(" |\n");
        md.append("| 监测重点 | ").append(value(adjustment.getMonitoringFocus())).append(" |\n\n");
    }

    private void appendEvidenceChain(StringBuilder md, ReportIntelligence intelligence) {
        md.append("## 九、结论证据链\n\n");
        List<EvidenceItem> evidence = safeList(intelligence.getEvidenceChain());
        if (evidence.isEmpty()) {
            md.append("暂无可展示的证据链。\n\n");
            return;
        }
        md.append("| 报告结论 | 来源类型 | 来源说明 | 参考依据 |\n| --- | --- | --- | --- |\n");
        for (EvidenceItem item : evidence) {
            md.append("| ")
                    .append(value(item.getConclusion())).append(" | ")
                    .append(value(item.getSourceType())).append(" | ")
                    .append(value(item.getSourceDetail())).append(" | ")
                    .append(safeList(item.getReferenceSources()).isEmpty() ? "暂无" : String.join("、", safeList(item.getReferenceSources())))
                    .append(" |\n");
        }
        md.append("\n");
    }

    private MetricTrendSummary buildMetricTrend(ReportContext context) {
        MetricTrendSummary trend = new MetricTrendSummary();
        List<HealthMetricDTO> metrics = context.getMetrics().stream()
                .filter(metric -> metric.getRecordedAt() != null)
                .sorted(Comparator.comparing(HealthMetricDTO::getRecordedAt))
                .toList();
        if (metrics.size() < 2) {
            trend.setPlainSummary(context.getLatestMetric() == null ? "暂无可分析的近期指标趋势。" : "目前只有少量指标记录，建议继续连续记录后再观察趋势。");
            trend.setFastingGlucoseTrend("数据不足");
            trend.setWeightTrend("数据不足");
            trend.setBloodPressureTrend("数据不足");
            return trend;
        }
        HealthMetricDTO first = metrics.get(0);
        HealthMetricDTO last = metrics.get(metrics.size() - 1);
        trend.setFastingGlucoseTrend(compareDecimal(first.getFastingGlucose(), last.getFastingGlucose(), "mmol/L"));
        trend.setWeightTrend(compareDecimal(first.getWeightKg(), last.getWeightKg(), "kg"));
        trend.setBloodPressureTrend(compareBloodPressure(first, last));
        trend.setPlainSummary("近" + context.getPeriodDays() + "天共有 " + metrics.size() + " 条指标记录；空腹血糖" + trend.getFastingGlucoseTrend() + "，体重" + trend.getWeightTrend() + "，血压" + trend.getBloodPressureTrend() + "。");
        return trend;
    }

    private List<String> buildPatientActions(ReportContext context) {
        List<String> actions = new ArrayList<>();
        actions.add("今天先完成饮食和运动打卡，重点记录主食、甜饮、加餐和运动时长。");
        if (context.getLatestMetric() == null || context.getLatestMetric().getFastingGlucose() == null) {
            actions.add("补一次空腹血糖记录；如果有条件，也补充餐后血糖或 HbA1c。");
        } else if (context.getLatestMetric().getFastingGlucose().compareTo(BigDecimal.valueOf(7.0)) >= 0) {
            actions.add("近期空腹血糖偏高，建议连续记录并准备线下复查材料。");
        }
        if (context.getCompletionRate() == null || context.getCompletionRate().compareTo(BigDecimal.valueOf(60)) < 0) {
            actions.add("把任务先减小：运动从 10 到 15 分钟开始，优先保证能坚持。");
        } else {
            actions.add("继续保持当前执行节奏，下一周期可做轻微优化，不需要频繁推翻原方案。");
        }
        return actions;
    }

    private List<String> buildAbnormalIndicators(HealthMetricDTO metric) {
        List<String> abnormal = new ArrayList<>();
        if (metric == null) {
            return List.of("暂无近期健康指标，无法判断异常指标。");
        }
        if (metric.getFastingGlucose() != null && metric.getFastingGlucose().compareTo(BigDecimal.valueOf(7.0)) >= 0) {
            abnormal.add("空腹血糖 " + metric.getFastingGlucose() + " mmol/L，达到或超过 7.0 mmol/L，建议线下复查确认。");
        }
        if (metric.getPostprandialGlucose() != null && metric.getPostprandialGlucose().compareTo(BigDecimal.valueOf(11.1)) >= 0) {
            abnormal.add("餐后血糖 " + metric.getPostprandialGlucose() + " mmol/L，达到或超过 11.1 mmol/L，建议线下复查确认。");
        }
        if (metric.getHba1c() != null && metric.getHba1c().compareTo(BigDecimal.valueOf(6.5)) >= 0) {
            abnormal.add("HbA1c " + metric.getHba1c() + "%，建议结合医生意见进一步评估。");
        }
        if (metric.getSystolicBp() != null && metric.getSystolicBp() >= 140 || metric.getDiastolicBp() != null && metric.getDiastolicBp() >= 90) {
            abnormal.add("血压 " + value(metric.getSystolicBp()) + "/" + value(metric.getDiastolicBp()) + " mmHg 偏高，运动强度建议保守。");
        }
        if (abnormal.isEmpty()) {
            abnormal.add("本次最新指标未识别到明显高危阈值，但仍需结合连续趋势和线下检查判断。");
        }
        return abnormal;
    }

    private List<EvidenceItem> buildEvidenceChain(ReportContext context) {
        List<EvidenceItem> evidence = new ArrayList<>();
        evidence.add(evidence("风险等级与风险因素来自最近一次风险预测结果。", "risk_assessments", context.getRiskAssessment() == null ? "暂无风险预测记录" : value(context.getRiskAssessment().getSummary()), List.of("Dify 风险预测工作流", "糖尿病风险评估结构化输出")));
        evidence.add(evidence("血糖、血压、体重和腰围判断来自近期健康指标。", "health_metrics", "本周期共 " + context.getMetrics().size() + " 条指标记录", List.of("用户健康指标记录")));
        evidence.add(evidence("生活干预建议来自当前生活方案和打卡执行反馈。", "life_plans + checkin_analysis", context.getCheckinAnalysis() == null ? "暂无打卡分析" : value(context.getCheckinAnalysis().getSummary()), List.of("个性化生活方案工作流", "打卡行为分析工作流")));
        if (StringUtils.hasText(context.getAiChatSummary()) && !context.getAiChatSummary().startsWith("No ")) {
            evidence.add(evidence("就医沟通关注点参考了 AI 医生咨询摘要。", "ai_chat_sessions", context.getAiChatSummary(), List.of("AI 医生咨询摘要")));
        }
        return evidence;
    }

    private EvidenceItem evidence(String conclusion, String sourceType, String sourceDetail, List<String> references) {
        EvidenceItem item = new EvidenceItem();
        item.setConclusion(conclusion);
        item.setSourceType(sourceType);
        item.setSourceDetail(sourceDetail);
        item.setReferenceSources(references);
        return item;
    }

    private List<String> buildSafetyWarnings(ReportContext context) {
        List<String> warnings = new ArrayList<>();
        warnings.add(DISCLAIMER);
        HealthMetricDTO metric = context.getLatestMetric();
        if (metric != null && metric.getFastingGlucose() != null && metric.getFastingGlucose().compareTo(BigDecimal.valueOf(7.0)) >= 0) {
            warnings.add("空腹血糖偏高时，应结合线下复查和医生意见判断，不建议只根据 AI 报告自行处理。");
        }
        if (metric != null && (metric.getSystolicBp() != null && metric.getSystolicBp() >= 140 || metric.getDiastolicBp() != null && metric.getDiastolicBp() >= 90)) {
            warnings.add("血压偏高时，运动建议应保守，避免突然增加高强度运动。");
        }
        warnings.add("报告不包含药物剂量、处方调整或确诊结论。");
        return warnings;
    }

    private String buildDietFocus(ReportContext context) {
        CheckinAnalysisDTO analysis = context.getCheckinAnalysis();
        if (analysis != null && StringUtils.hasText(analysis.getMainProblems()) && analysis.getMainProblems().contains("饮食")) {
            return "优先处理饮食打卡中反复出现的问题，减少甜饮、夜宵和高油高糖加餐，给出可替代食物。";
        }
        return "继续保持三餐规律和主食定量，下一周期重点观察餐后血糖与晚餐结构。";
    }

    private String buildExerciseFocus(ReportContext context) {
        if (context.getCompletionRate() == null || context.getCompletionRate().compareTo(BigDecimal.valueOf(60)) < 0) {
            return "完成率偏低时先下调运动强度和时长，从饭后散步 10 到 15 分钟开始。";
        }
        return "保持中低强度有氧运动，若血压或血糖异常则避免突然增加强度。";
    }

    private String buildMonitoringFocus(ReportContext context) {
        if (context.getMissingItems().isEmpty()) {
            return "继续记录空腹血糖、餐后血糖、体重腰围和血压，重点看连续趋势。";
        }
        return "优先补齐" + String.join("、", context.getMissingItems()) + "，否则下一周期报告的解释能力会受限。";
    }

    private String compareDecimal(BigDecimal first, BigDecimal last, String unit) {
        if (first == null || last == null) {
            return "数据不足";
        }
        BigDecimal diff = last.subtract(first).setScale(1, RoundingMode.HALF_UP);
        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            return "较期初上升 " + diff + " " + unit;
        }
        if (diff.compareTo(BigDecimal.ZERO) < 0) {
            return "较期初下降 " + diff.abs() + " " + unit;
        }
        return "较期初基本持平";
    }

    private String compareBloodPressure(HealthMetricDTO first, HealthMetricDTO last) {
        if (first.getSystolicBp() == null || last.getSystolicBp() == null) {
            return "数据不足";
        }
        int diff = last.getSystolicBp() - first.getSystolicBp();
        if (diff > 0) {
            return "收缩压较期初上升 " + diff + " mmHg";
        }
        if (diff < 0) {
            return "收缩压较期初下降 " + Math.abs(diff) + " mmHg";
        }
        return "收缩压较期初基本持平";
    }

    private List<String> compactList(List<String> values) {
        return values.stream().filter(StringUtils::hasText).toList();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private Object firstPresent(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            if (values.containsKey(key) && values.get(key) != null) {
                return values.get(key);
            }
        }
        return null;
    }

    private String stripJsonFence(String text) {
        String stripped = value(text).trim();
        stripped = stripped.replaceAll("(?is)^```json\\s*", "").replaceAll("(?is)^```\\s*", "").replaceAll("(?is)```$", "").trim();
        int start = stripped.indexOf('{');
        int end = stripped.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return stripped.substring(start, end + 1);
        }
        return stripped;
    }

    private String toJsonOrEmpty(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
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
        String markdown = stripCompletenessScore(report.getReportMarkdown());
        return markdown.replaceAll("(?m)^- 追溯链接：.*$", "- 追溯链接：" + buildTraceUrl(report.getReportId()));
    }

    private String buildPublicHtml(HealthReport report) {
        String body = markdownToHtml(displayMarkdown(report));
        String summary = escapeHtml(value(report.getReportSummary()));
        String title = escapeHtml(value(report.getReportTitle()));
        String publicPdfUrl = "/api/reports/public/" + report.getReportId() + "/pdf";
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                  <style>
                    * { box-sizing: border-box; }
                    html { -webkit-text-size-adjust: 100%%; }
                    body { margin: 0; background: #F7FCF9; color: #24323D; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans SC", "Microsoft YaHei", sans-serif; }
                    main { width: min(100%%, 820px); margin: 0 auto; padding: 14px 12px 34px; }
                    .toolbar { position: sticky; top: 0; z-index: 10; display: flex; justify-content: flex-end; padding: 8px 0 10px; background: rgba(247, 252, 249, .94); backdrop-filter: blur(10px); }
                    .download-btn { display: inline-flex; align-items: center; justify-content: center; min-height: 38px; padding: 0 14px; border-radius: 999px; background: #4A8A6A; color: #fff; font-size: 14px; font-weight: 800; text-decoration: none; box-shadow: 0 8px 18px rgba(74, 138, 106, .20); }
                    header, article { border: 1px solid rgba(174, 232, 199, .42); border-radius: 16px; background: #fff; box-shadow: 0 8px 28px rgba(140, 190, 170, .10); }
                    header { padding: 20px 18px; background: linear-gradient(145deg, #FFFFFF 0%%, #EDF8F4 58%%, #EAF5FA 100%%); }
                    .report-id { color: #4A8A6A; font-size: 13px; font-weight: 800; }
                    h1 { margin: 8px 0; color: #172635; font-size: 24px; line-height: 1.32; }
                    .summary { margin: 0; color: #687789; font-size: 14px; line-height: 1.75; }
                    .time { display: block; margin-top: 12px; color: #82959E; font-size: 12px; }
                    article { margin-top: 14px; padding: 18px; font-size: 15px; line-height: 1.9; overflow-wrap: anywhere; }
                    article h1 { font-size: 22px; }
                    article h2 { margin: 22px 0 10px; padding-top: 10px; border-top: 1px solid rgba(174, 232, 199, .5); color: #24323D; font-size: 18px; }
                    article h3 { margin: 16px 0 8px; color: #24323D; font-size: 16px; }
                    article p { margin: 0 0 10px; }
                    article blockquote { margin: 10px 0 14px; padding: 10px 12px; border-left: 4px solid #6FCF97; border-radius: 0 10px 10px 0; background: #F0FAF5; color: #526575; }
                    article ul, article ol { padding-left: 20px; }
                    article li { margin: 4px 0; }
                    article table { display: block; width: 100%%; margin: 12px 0; overflow-x: auto; border-collapse: collapse; font-size: 13px; }
                    article th, article td { padding: 8px 9px; border: 1px solid rgba(174, 232, 199, .65); text-align: left; vertical-align: top; }
                    article th { background: #EAF8F1; }
                    @media (max-width: 900px) {
                      body { background: #fff; }
                      main { width: 100%%; max-width: none; padding: 0 0 22px; }
                      .toolbar { padding: 8px 12px; border-bottom: 1px solid rgba(174, 232, 199, .36); }
                      .download-btn { min-height: 40px; padding: 0 16px; font-size: 15px; }
                      header, article { border-left: 0; border-right: 0; border-radius: 0; box-shadow: none; }
                      header { padding: 18px 16px; }
                      article { margin-top: 0; padding: 18px 16px 30px; font-size: 17px; line-height: 2.0; }
                      h1 { font-size: 24px; }
                      .summary { font-size: 15px; }
                      article h1 { font-size: 23px; }
                      article h2 { font-size: 20px; }
                      article h3 { font-size: 18px; }
                      article table { font-size: 15px; }
                      article th, article td { padding: 10px 11px; }
                    }
                  </style>
                </head>
                <body>
                  <main>
                    <nav class="toolbar">
                      <a class="download-btn" href="%s" download="report-%s.pdf">下载 PDF</a>
                    </nav>
                    <header>
                      <span class="report-id">RPT%s</span>
                      <h1>%s</h1>
                      <p class="summary">%s</p>
                      <span class="time">生成时间：%s</span>
                    </header>
                    <article>%s</article>
                  </main>
                </body>
                </html>
                """.formatted(title, publicPdfUrl, report.getReportId(), String.format("%04d", report.getReportId()), title, summary, formatDateTime(report.getCreateTime()), body);
    }

    private String markdownToHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        String[] lines = markdown.split("\\R");
        boolean inTable = false;
        boolean inList = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!StringUtils.hasText(line)) {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                if (inTable) {
                    html.append("</tbody></table>");
                    inTable = false;
                }
                continue;
            }
            if (line.startsWith("|") && line.endsWith("|")) {
                if (i + 1 < lines.length && lines[i + 1].trim().matches("^\\|\\s*-+.*")) {
                    if (inList) {
                        html.append("</ul>");
                        inList = false;
                    }
                    html.append("<table><thead><tr>");
                    for (String cell : tableCells(line)) {
                        html.append("<th>").append(escapeHtml(cell)).append("</th>");
                    }
                    html.append("</tr></thead><tbody>");
                    inTable = true;
                    i++;
                    continue;
                }
                if (inTable) {
                    html.append("<tr>");
                    for (String cell : tableCells(line)) {
                        html.append("<td>").append(escapeHtml(cell)).append("</td>");
                    }
                    html.append("</tr>");
                    continue;
                }
            } else if (inTable) {
                html.append("</tbody></table>");
                inTable = false;
            }
            if (line.startsWith("- ")) {
                if (!inList) {
                    html.append("<ul>");
                    inList = true;
                }
                html.append("<li>").append(escapeHtml(line.substring(2))).append("</li>");
                continue;
            } else if (inList) {
                html.append("</ul>");
                inList = false;
            }
            if (line.startsWith("### ")) {
                html.append("<h3>").append(escapeHtml(line.substring(4))).append("</h3>");
            } else if (line.startsWith("## ")) {
                html.append("<h2>").append(escapeHtml(line.substring(3))).append("</h2>");
            } else if (line.startsWith("# ")) {
                html.append("<h1>").append(escapeHtml(line.substring(2))).append("</h1>");
            } else if (line.startsWith("> ")) {
                html.append("<blockquote>").append(escapeHtml(line.substring(2))).append("</blockquote>");
            } else {
                html.append("<p>").append(escapeHtml(line)).append("</p>");
            }
        }
        if (inList) {
            html.append("</ul>");
        }
        if (inTable) {
            html.append("</tbody></table>");
        }
        return html.toString();
    }

    private List<String> tableCells(String line) {
        String inner = line.substring(1, line.length() - 1);
        String[] cells = inner.split("\\|");
        List<String> result = new ArrayList<>();
        for (String cell : cells) {
            result.add(cell.trim());
        }
        return result;
    }

    private String escapeHtml(String text) {
        return value(text)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
        return baseUrl + "/api/reports/public/" + reportId + "/html";
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
        private ReportIntelligence intelligence;
    }

    @Data
    public static class ReportIntelligence {
        @JsonProperty("patient_report")
        private PatientReport patientReport;
        @JsonProperty("doctor_report")
        private DoctorReport doctorReport;
        @JsonProperty("evidence_chain")
        private List<EvidenceItem> evidenceChain = List.of();
        @JsonProperty("data_gaps")
        private List<String> dataGaps = List.of();
        @JsonProperty("safety_warnings")
        private List<String> safetyWarnings = List.of();
        @JsonProperty("next_cycle_adjustment")
        private NextCycleAdjustment nextCycleAdjustment;
        @JsonProperty("metric_trend")
        private MetricTrendSummary metricTrend;
        private String summary;
        @JsonProperty("generation_mode")
        private String generationMode;
    }

    @Data
    public static class PatientReport {
        @JsonProperty("friendly_summary")
        private String friendlySummary;
        @JsonProperty("key_findings")
        private List<String> keyFindings = List.of();
        @JsonProperty("today_action")
        private List<String> todayAction = List.of();
        private String encouragement;
    }

    @Data
    public static class DoctorReport {
        @JsonProperty("chief_concern")
        private String chiefConcern;
        @JsonProperty("risk_summary")
        private String riskSummary;
        @JsonProperty("abnormal_indicators")
        private List<String> abnormalIndicators = List.of();
        @JsonProperty("behavior_adherence")
        private String behaviorAdherence;
        @JsonProperty("clinical_communication_points")
        private List<String> clinicalCommunicationPoints = List.of();
        @JsonProperty("follow_up_suggestions")
        private List<String> followUpSuggestions = List.of();
    }

    @Data
    public static class EvidenceItem {
        private String conclusion;
        @JsonProperty("source_type")
        private String sourceType;
        @JsonProperty("source_detail")
        private String sourceDetail;
        @JsonProperty("reference_sources")
        private List<String> referenceSources = List.of();
    }

    @Data
    public static class NextCycleAdjustment {
        @JsonProperty("diet_focus")
        private String dietFocus;
        @JsonProperty("exercise_focus")
        private String exerciseFocus;
        @JsonProperty("monitoring_focus")
        private String monitoringFocus;
    }

    @Data
    public static class MetricTrendSummary {
        @JsonProperty("plain_summary")
        private String plainSummary;
        @JsonProperty("fasting_glucose_trend")
        private String fastingGlucoseTrend;
        @JsonProperty("weight_trend")
        private String weightTrend;
        @JsonProperty("blood_pressure_trend")
        private String bloodPressureTrend;
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
            String safeText = sanitizePdfText(text);
            if (!StringUtils.hasText(safeText)) {
                y -= LINE_HEIGHT * spacingMultiplier;
                return;
            }
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(safeText);
            stream.endText();
            y -= LINE_HEIGHT * spacingMultiplier;
        }

        private String sanitizePdfText(String text) {
            if (text == null) {
                return "";
            }
            return text.codePoints()
                    .filter(codePoint -> Character.UnicodeBlock.of(codePoint) != Character.UnicodeBlock.EMOTICONS)
                    .filter(codePoint -> Character.UnicodeBlock.of(codePoint) != Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS)
                    .filter(codePoint -> Character.UnicodeBlock.of(codePoint) != Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString()
                    .replace("✨", "")
                    .replace("🌿", "")
                    .replace("📊", "")
                    .trim();
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
