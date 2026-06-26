package com.diabetes.assistant.modules.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.aichat.dto.AiChatHistoryMessageResponse;
import com.diabetes.assistant.modules.aichat.dto.AiChatMessageRequest;
import com.diabetes.assistant.modules.aichat.dto.AiChatMessageResponse;
import com.diabetes.assistant.modules.aichat.dto.AiChatSessionResponse;
import com.diabetes.assistant.modules.aichat.dto.admin.AdminAiChatLogResponse;
import com.diabetes.assistant.modules.aichat.dto.admin.AdminAiChatQuery;
import com.diabetes.assistant.modules.aichat.dto.admin.AdminAiChatSessionResponse;
import com.diabetes.assistant.modules.aichat.entity.AiChatMessage;
import com.diabetes.assistant.modules.aichat.entity.AiChatSession;
import com.diabetes.assistant.modules.aichat.mapper.AiChatMessageMapper;
import com.diabetes.assistant.modules.aichat.mapper.AiChatSessionMapper;
import com.diabetes.assistant.modules.aichat.service.AiChatService;
import com.diabetes.assistant.modules.checkin.contract.CheckinQueryApi;
import com.diabetes.assistant.modules.checkin.contract.dto.CheckinAnalysisDTO;
import com.diabetes.assistant.modules.checkin.contract.dto.CheckinRecordDTO;
import com.diabetes.assistant.modules.checkin.entity.ApiCallLog;
import com.diabetes.assistant.modules.checkin.mapper.ApiCallLogMapper;
import com.diabetes.assistant.modules.dify.service.DifyService;
import com.diabetes.assistant.modules.healthmetric.contract.HealthMetricQueryApi;
import com.diabetes.assistant.modules.lifeplan.contract.LifePlanQueryApi;
import com.diabetes.assistant.modules.profile.contract.PatientProfileQueryApi;
import com.diabetes.assistant.modules.risk.contract.RiskAssessmentQueryApi;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import com.diabetes.assistant.modules.user.contract.dto.UserBasicDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private static final String SESSION_STATUS_ACTIVE = "active";
    private static final String SESSION_STATUS_DELETED = "deleted";
    private static final String CALL_STATUS_SUCCESS = "success";
    private static final String CALL_STATUS_FAILED = "failed";
    private static final String SERVICE_AI_CHAT = "ai_chat";
    private static final int CONTEXT_PERIOD_DAYS = 7;
    private static final int MAX_MESSAGE_LENGTH = 2000;

    private static final String FALLBACK_ANSWER = "AI 医生暂时不可用，请稍后再试。你可以先记录血糖、饮食、运动和不适症状；如果出现明显不适或血糖读数异常，请尽快线下就医或复查。";

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final UserQueryApi userQueryApi;
    private final PatientProfileQueryApi patientProfileQueryApi;
    private final HealthMetricQueryApi healthMetricQueryApi;
    private final RiskAssessmentQueryApi riskAssessmentQueryApi;
    private final LifePlanQueryApi lifePlanQueryApi;
    private final CheckinQueryApi checkinQueryApi;
    private final DifyService difyService;
    private final ApiCallLogMapper apiCallLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String entry() {
        return "AI doctor consultation module";
    }

    @Override
    @Transactional
    public AiChatMessageResponse sendMessage(Integer userId, AiChatMessageRequest request) {
        if (!StringUtils.hasText(request.getMessage())) {
            throw new BusinessException("Message is required");
        }
        String userMessage = normalizeUserMessage(request.getMessage());

        AiChatSession session = resolveSession(userId, request.getSessionId(), userMessage);
        Map<String, Object> context = buildDifyContext(userId);
        String contextSummary = toJson(context);

        String answer;
        String conversationId = request.getSessionId() != null && StringUtils.hasText(request.getConversationId())
                ? request.getConversationId()
                : session.getDifyConversationId();
        String callStatus = CALL_STATUS_SUCCESS;
        String errorMessage = null;

        try {
            String rawResponse = difyService.callAiDoctor(userMessage, conversationId, context, String.valueOf(userId));
            ParsedAiResponse parsed = parseDifyResponse(rawResponse);
            answer = StringUtils.hasText(parsed.answer()) ? parsed.answer() : FALLBACK_ANSWER;
            conversationId = StringUtils.hasText(parsed.conversationId()) ? parsed.conversationId() : conversationId;
        } catch (Exception exception) {
            callStatus = CALL_STATUS_FAILED;
            errorMessage = sanitizeError(exception);
            answer = FALLBACK_ANSWER;
        }

        AiChatMessage saved = saveMessage(userId, session.getSessionId(), userMessage, answer,
                contextSummary, callStatus, errorMessage);
        touchSession(session, userMessage, conversationId, saved.getCreateTime());
        saveCallLog(userId, userMessage, answer, callStatus, errorMessage);

        AiChatMessageResponse response = new AiChatMessageResponse();
        response.setSessionId(session.getSessionId());
        response.setMessageId(saved.getMessageId());
        response.setConversationId(conversationId);
        response.setUserMessage(saved.getUserMessage());
        response.setAnswer(saved.getAiResponse());
        response.setContextSummary(saved.getContextSummary());
        response.setCallStatus(saved.getCallStatus());
        response.setErrorMessage(saved.getErrorMessage());
        response.setCreateTime(saved.getCreateTime());
        return response;
    }

    @Override
    public PageResult<AiChatSessionResponse> listSessions(Integer userId, Integer page, Integer pageSize) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        Page<AiChatSession> result = sessionMapper.selectPage(Page.of(normalizedPage, normalizedPageSize),
                new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .eq(AiChatSession::getStatus, SESSION_STATUS_ACTIVE)
                        .orderByDesc(AiChatSession::getLastMessageTime)
                        .orderByDesc(AiChatSession::getCreateTime));
        List<AiChatSessionResponse> list = result.getRecords().stream()
                .map(this::toSessionResponse)
                .toList();
        return new PageResult<>(list, result.getTotal(), normalizedPage, normalizedPageSize);
    }

    @Override
    public List<AiChatHistoryMessageResponse> listMessages(Integer userId, Integer sessionId) {
        requireOwnedActiveSession(userId, sessionId);
        return messageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .eq(AiChatMessage::getUserId, userId)
                        .orderByAsc(AiChatMessage::getCreateTime)
                        .orderByAsc(AiChatMessage::getMessageId))
                .stream()
                .map(this::toHistoryMessageResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteSession(Integer userId, Integer sessionId) {
        AiChatSession session = requireOwnedActiveSession(userId, sessionId);
        session.setStatus(SESSION_STATUS_DELETED);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    @Override
    @Transactional
    public void clearSession(Integer userId, Integer sessionId) {
        AiChatSession session = requireOwnedActiveSession(userId, sessionId);
        messageMapper.delete(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .eq(AiChatMessage::getUserId, userId));
        session.setLastMessageTime(null);
        session.setDifyConversationId(null);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    @Override
    public PageResult<AdminAiChatLogResponse> listAdminLogs(AdminAiChatQuery query) {
        int page = normalizePage(query.getPage());
        int pageSize = normalizePageSize(query.getPageSize());
        LambdaQueryWrapper<AiChatMessage> wrapper = buildAdminMessageWrapper(query)
                .orderByDesc(AiChatMessage::getCreateTime)
                .orderByDesc(AiChatMessage::getMessageId);
        Page<AiChatMessage> result = messageMapper.selectPage(Page.of(page, pageSize), wrapper);
        Map<Integer, AiChatSession> sessions = loadSessions(result.getRecords().stream()
                .map(AiChatMessage::getSessionId)
                .collect(Collectors.toSet()));
        List<AdminAiChatLogResponse> list = result.getRecords().stream()
                .map(message -> toAdminLogResponse(message, sessions.get(message.getSessionId())))
                .toList();
        return new PageResult<>(list, result.getTotal(), page, pageSize);
    }

    @Override
    public PageResult<AdminAiChatSessionResponse> listAdminSessions(AdminAiChatQuery query) {
        int page = normalizePage(query.getPage());
        int pageSize = normalizePageSize(query.getPageSize());
        LambdaQueryWrapper<AiChatSession> wrapper = buildAdminSessionWrapper(query)
                .orderByDesc(AiChatSession::getLastMessageTime)
                .orderByDesc(AiChatSession::getCreateTime);
        Page<AiChatSession> result = sessionMapper.selectPage(Page.of(page, pageSize), wrapper);
        List<AdminAiChatSessionResponse> list = result.getRecords().stream()
                .map(this::toAdminSessionResponse)
                .toList();
        return new PageResult<>(list, result.getTotal(), page, pageSize);
    }

    @Override
    public AdminAiChatLogResponse getAdminMessageDetail(Integer messageId) {
        AiChatMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(404, "AI chat message does not exist");
        }
        AiChatSession session = sessionMapper.selectById(message.getSessionId());
        return toAdminLogResponse(message, session);
    }

    private AiChatSession resolveSession(Integer userId, Integer sessionId, String firstMessage) {
        if (sessionId != null) {
            AiChatSession existing = sessionMapper.selectById(sessionId);
            if (existing == null || !Objects.equals(existing.getUserId(), userId)
                || SESSION_STATUS_DELETED.equals(existing.getStatus())) {
                throw new BusinessException(404, "Session does not exist or has been deleted");
            }
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setSessionTitle(buildSessionTitle(firstMessage));
        session.setStatus(SESSION_STATUS_ACTIVE);
        session.setLastMessageTime(now);
        session.setCreateTime(now);
        session.setUpdateTime(now);
        sessionMapper.insert(session);
        return session;
    }

    private AiChatSession requireOwnedActiveSession(Integer userId, Integer sessionId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !Objects.equals(session.getUserId(), userId)
            || SESSION_STATUS_DELETED.equals(session.getStatus())) {
            throw new BusinessException(404, "Session does not exist or has been deleted");
        }
        return session;
    }

    private Map<String, Object> buildDifyContext(Integer userId) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("safety_rules", List.of(
                "You are the AI doctor consultation agent for the Diabetes Prevention and Care Assistant, not a real doctor.",
                "Do not make final diagnoses, prescriptions, or medication dosage instructions.",
                "Do not invent missing medical history, medication use, or test results.",
                "For severe symptoms or clearly abnormal indicators, advise offline medical care or recheck."
        ));
        context.put("user_basic", buildUserBasic(userId));
        context.put("profile_summary", safeSummary(() -> patientProfileQueryApi.getProfileSummaryByUserId(userId), "No health profile"));
        context.put("latest_health_data", safeSummary(() -> healthMetricQueryApi.getLatestMetricSummaryByUserId(userId), "No latest health data"));
        context.put("risk_result", safeSummary(() -> riskAssessmentQueryApi.getLatestRiskSummaryByUserId(userId), "No risk assessment result"));
        context.put("life_plan", safeSummary(() -> lifePlanQueryApi.getCurrentLifePlanSummaryByUserId(userId), "No life plan"));
        context.put("checkin", buildCheckinContext(userId));
        return context;
    }

    private Map<String, Object> buildUserBasic(Integer userId) {
        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("user_id", userId);
        try {
            UserBasicDTO user = userQueryApi.getUserBasicById(userId);
            if (user != null) {
                basic.put("username", user.getUsername());
                basic.put("role", user.getRole());
                basic.put("status", user.getStatus());
            }
        } catch (Exception ignored) {
            basic.put("summary", "No user basic data");
        }
        return basic;
    }

    private Map<String, Object> buildCheckinContext(Integer userId) {
        Map<String, Object> checkin = new LinkedHashMap<>();
        checkin.put("period_days", CONTEXT_PERIOD_DAYS);
        checkin.put("recent_summary", safeSummary(
                () -> checkinQueryApi.getLatestCheckinSummaryByUserId(userId, CONTEXT_PERIOD_DAYS),
                "No recent check-in summary"));
        try {
            checkin.put("completion_rate", checkinQueryApi.getRecentCompletionRate(userId, CONTEXT_PERIOD_DAYS));
        } catch (Exception exception) {
            checkin.put("completion_rate", null);
        }
        try {
            List<CheckinRecordDTO> recentRecords = checkinQueryApi.listRecentCheckins(userId, CONTEXT_PERIOD_DAYS);
            checkin.put("recent_records", recentRecords.stream().map(this::toRecordContext).toList());
        } catch (Exception exception) {
            checkin.put("recent_records", List.of());
        }
        try {
            CheckinAnalysisDTO latestAnalysis = checkinQueryApi.getLatestAnalysisByUserId(userId);
            checkin.put("latest_analysis", latestAnalysis == null ? "No check-in analysis result" : toAnalysisContext(latestAnalysis));
        } catch (Exception exception) {
            checkin.put("latest_analysis", "No check-in analysis result");
        }
        return checkin;
    }

    private Map<String, Object> toRecordContext(CheckinRecordDTO record) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("checkin_id", record.getCheckinId());
        value.put("date", record.getCheckinDate());
        value.put("task_type", record.getTaskType());
        value.put("task_name", record.getTaskName());
        value.put("status", record.getStatus());
        value.put("note", record.getNote());
        return value;
    }

    private Map<String, Object> toAnalysisContext(CheckinAnalysisDTO analysis) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", analysis.getAnalysisId());
        value.put("start_date", analysis.getStartDate());
        value.put("end_date", analysis.getEndDate());
        value.put("total_days", analysis.getTotalDays());
        value.put("diet_completion_count", analysis.getDietCompletionCount());
        value.put("exercise_completion_count", analysis.getExerciseCompletionCount());
        value.put("completion_rate", analysis.getCompletionRate());
        value.put("habit_score", analysis.getHabitScore());
        value.put("diet_summary", analysis.getDietSummary());
        value.put("exercise_summary", analysis.getExerciseSummary());
        value.put("life_evaluation", analysis.getLifeEvaluation());
        value.put("main_problems", analysis.getMainProblems());
        value.put("improvement_suggestions", analysis.getImprovementSuggestions());
        value.put("next_focus", analysis.getNextFocus());
        value.put("summary", analysis.getSummary());
        value.put("call_status", analysis.getCallStatus());
        value.put("error_message", analysis.getErrorMessage());
        return value;
    }

    private ParsedAiResponse parseDifyResponse(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode data = root.path("data");
        String answer = firstText(root.path("answer"), data.path("answer"));
        String conversationId = firstText(root.path("conversation_id"), data.path("conversation_id"));
        return new ParsedAiResponse(sanitizeAiAnswer(answer), conversationId);
    }

    private String sanitizeAiAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        return answer.replaceAll("(?is)<think>.*?</think>\\s*", "").trim();
    }

    private String firstText(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isMissingNode() && !node.isNull() && StringUtils.hasText(node.asText())) {
                return node.asText();
            }
        }
        return "";
    }

    private AiChatMessage saveMessage(Integer userId, Integer sessionId, String userMessage, String aiResponse,
                                      String contextSummary, String callStatus, String errorMessage) {
        AiChatMessage message = new AiChatMessage();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setUserMessage(userMessage);
        message.setAiResponse(aiResponse);
        message.setContextSummary(truncate(contextSummary, 4000));
        message.setCallStatus(callStatus);
        message.setErrorMessage(truncate(errorMessage, 1000));
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    private void touchSession(AiChatSession session, String message, String conversationId, LocalDateTime lastMessageTime) {
        if (!StringUtils.hasText(session.getSessionTitle())) {
            session.setSessionTitle(buildSessionTitle(message));
        }
        if (StringUtils.hasText(conversationId)) {
            session.setDifyConversationId(conversationId);
        }
        session.setLastMessageTime(lastMessageTime);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    private String normalizeUserMessage(String message) {
        String normalized = message == null ? "" : message.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("Message is required");
        }
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new BusinessException(400, "Message is too long");
        }
        return normalized;
    }

    private String buildSessionTitle(String message) {
        String normalized = message == null ? "AI doctor consultation" : message.trim().replaceAll("\\s+", " ");
        if (!StringUtils.hasText(normalized)) {
            return "AI doctor consultation";
        }
        return normalized.length() <= 30 ? normalized : normalized.substring(0, 30);
    }

    private String safeSummary(SummarySupplier supplier, String fallback) {
        try {
            String value = supplier.get();
            return StringUtils.hasText(value) ? value : fallback;
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private String sanitizeError(Exception exception) {
        String message = exception == null ? "" : exception.getMessage();
        if (!StringUtils.hasText(message)) {
            return "AI doctor call failed";
        }
        String sanitized = message.replaceAll("Bearer\\s+[^\\s]+", "Bearer ***");
        return truncate(sanitized, 1000);
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void saveCallLog(Integer userId, String requestSummary, String responseSummary, String callStatus,
                             String errorMessage) {
        ApiCallLog log = new ApiCallLog();
        log.setUserId(userId);
        log.setServiceType(SERVICE_AI_CHAT);
        log.setRequestSummary(truncate(requestSummary, 2000));
        log.setResponseSummary(truncate(responseSummary, 2000));
        log.setCallStatus(callStatus);
        log.setErrorMessage(truncate(errorMessage, 1000));
        log.setCreateTime(LocalDateTime.now());
        apiCallLogMapper.insert(log);
    }

    private AiChatSessionResponse toSessionResponse(AiChatSession session) {
        AiChatSessionResponse response = new AiChatSessionResponse();
        response.setSessionId(session.getSessionId());
        response.setSessionTitle(session.getSessionTitle());
        response.setDifyConversationId(session.getDifyConversationId());
        response.setStatus(session.getStatus());
        response.setLastMessageTime(session.getLastMessageTime());
        response.setCreateTime(session.getCreateTime());
        response.setUpdateTime(session.getUpdateTime());
        return response;
    }

    private AiChatHistoryMessageResponse toHistoryMessageResponse(AiChatMessage message) {
        AiChatHistoryMessageResponse response = new AiChatHistoryMessageResponse();
        response.setMessageId(message.getMessageId());
        response.setSessionId(message.getSessionId());
        response.setUserMessage(message.getUserMessage());
        response.setAiResponse(message.getAiResponse());
        response.setContextSummary(message.getContextSummary());
        response.setCallStatus(message.getCallStatus());
        response.setErrorMessage(message.getErrorMessage());
        response.setCreateTime(message.getCreateTime());
        return response;
    }

    private AdminAiChatLogResponse toAdminLogResponse(AiChatMessage message, AiChatSession session) {
        UserBasicDTO user = userQueryApi.getUserBasicById(message.getUserId());
        AdminAiChatLogResponse response = new AdminAiChatLogResponse();
        response.setMessageId(message.getMessageId());
        response.setSessionId(message.getSessionId());
        response.setUserId(message.getUserId());
        response.setUsername(user == null ? null : user.getUsername());
        response.setSessionTitle(session == null ? null : session.getSessionTitle());
        response.setUserMessage(message.getUserMessage());
        response.setAiResponse(message.getAiResponse());
        response.setContextSummary(message.getContextSummary());
        response.setCallStatus(message.getCallStatus());
        response.setErrorMessage(message.getErrorMessage());
        response.setCreateTime(message.getCreateTime());
        return response;
    }

    private AdminAiChatSessionResponse toAdminSessionResponse(AiChatSession session) {
        UserBasicDTO user = userQueryApi.getUserBasicById(session.getUserId());
        AdminAiChatSessionResponse response = new AdminAiChatSessionResponse();
        response.setSessionId(session.getSessionId());
        response.setUserId(session.getUserId());
        response.setUsername(user == null ? null : user.getUsername());
        response.setSessionTitle(session.getSessionTitle());
        response.setDifyConversationId(session.getDifyConversationId());
        response.setStatus(session.getStatus());
        response.setMessageCount(messageMapper.selectCount(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, session.getSessionId())));
        response.setLastMessageTime(session.getLastMessageTime());
        response.setCreateTime(session.getCreateTime());
        response.setUpdateTime(session.getUpdateTime());
        return response;
    }

    private LambdaQueryWrapper<AiChatMessage> buildAdminMessageWrapper(AdminAiChatQuery query) {
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getUserId() != null, AiChatMessage::getUserId, query.getUserId());
        wrapper.eq(StringUtils.hasText(query.getCallStatus()), AiChatMessage::getCallStatus, query.getCallStatus());
        wrapper.ge(query.getStartDate() != null, AiChatMessage::getCreateTime,
                query.getStartDate() == null ? null : query.getStartDate().atStartOfDay());
        wrapper.lt(query.getEndDate() != null, AiChatMessage::getCreateTime,
                query.getEndDate() == null ? null : query.getEndDate().plusDays(1).atStartOfDay());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(AiChatMessage::getUserMessage, query.getKeyword())
                    .or()
                    .like(AiChatMessage::getAiResponse, query.getKeyword()));
        }
        applyUserKeyword(wrapper, query.getUserKeyword(), AiChatMessage::getUserId);
        return wrapper;
    }

    private LambdaQueryWrapper<AiChatSession> buildAdminSessionWrapper(AdminAiChatQuery query) {
        LambdaQueryWrapper<AiChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getUserId() != null, AiChatSession::getUserId, query.getUserId());
        wrapper.eq(StringUtils.hasText(query.getCallStatus()), AiChatSession::getStatus, query.getCallStatus());
        wrapper.ge(query.getStartDate() != null, AiChatSession::getCreateTime,
                query.getStartDate() == null ? null : query.getStartDate().atStartOfDay());
        wrapper.lt(query.getEndDate() != null, AiChatSession::getCreateTime,
                query.getEndDate() == null ? null : query.getEndDate().plusDays(1).atStartOfDay());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(AiChatSession::getSessionTitle, query.getKeyword());
        }
        applyUserKeyword(wrapper, query.getUserKeyword(), AiChatSession::getUserId);
        return wrapper;
    }

    private <T> void applyUserKeyword(LambdaQueryWrapper<T> wrapper, String userKeyword,
                                      SFunction<T, ?> userIdColumn) {
        if (!StringUtils.hasText(userKeyword)) {
            return;
        }
        List<Integer> matchedUserIds = loadMatchedUserIds(userKeyword);
        if (matchedUserIds.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in(userIdColumn, matchedUserIds);
    }

    private List<Integer> loadMatchedUserIds(String keyword) {
        List<Integer> ids = new ArrayList<>();
        Set<Integer> relatedUserIds = new java.util.LinkedHashSet<>();
        messageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>().select(AiChatMessage::getUserId))
                .forEach(message -> relatedUserIds.add(message.getUserId()));
        sessionMapper.selectList(new LambdaQueryWrapper<AiChatSession>().select(AiChatSession::getUserId))
                .forEach(session -> relatedUserIds.add(session.getUserId()));
        for (Integer userId : relatedUserIds) {
            UserBasicDTO user = userQueryApi.getUserBasicById(userId);
            if (user == null) {
                continue;
            }
            if (contains(user.getUsername(), keyword) || contains(user.getPhone(), keyword) || contains(user.getEmail(), keyword)) {
                ids.add(userId);
            }
        }
        return ids;
    }

    private Map<Integer, AiChatSession> loadSessions(Set<Integer> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Map.of();
        }
        return sessionMapper.selectBatchIds(sessionIds).stream()
                .collect(Collectors.toMap(AiChatSession::getSessionId, Function.identity()));
    }

    private boolean contains(String value, String keyword) {
        return StringUtils.hasText(value) && value.contains(keyword);
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

    @FunctionalInterface
    private interface SummarySupplier {
        String get();
    }

    private record ParsedAiResponse(String answer, String conversationId) {
    }
}
