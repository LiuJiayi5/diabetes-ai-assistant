package com.diabetes.assistant.modules.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.utils.DateTimeUtil;
import com.diabetes.assistant.modules.content.dto.ArticleReadEventRequest;
import com.diabetes.assistant.modules.content.entity.Article;
import com.diabetes.assistant.modules.content.entity.ArticleReadEvent;
import com.diabetes.assistant.modules.content.entity.ArticleRecommendationLog;
import com.diabetes.assistant.modules.content.entity.ArticleTag;
import com.diabetes.assistant.modules.content.mapper.ArticleMapper;
import com.diabetes.assistant.modules.content.mapper.ArticleReadEventMapper;
import com.diabetes.assistant.modules.content.mapper.ArticleRecommendationLogMapper;
import com.diabetes.assistant.modules.content.mapper.ArticleTagMapper;
import com.diabetes.assistant.modules.content.service.ArticleRecommendationService;
import com.diabetes.assistant.modules.content.vo.ArticleReadEventResponse;
import com.diabetes.assistant.modules.content.vo.ArticleRecommendationResponse;
import com.diabetes.assistant.modules.content.vo.ArticleResponse;
import com.diabetes.assistant.modules.content.vo.PatientEducationProfileResponse;
import com.diabetes.assistant.modules.dify.dto.DifyWorkflowResult;
import com.diabetes.assistant.modules.dify.service.DifyService;
import com.diabetes.assistant.modules.healthmetric.entity.HealthMetric;
import com.diabetes.assistant.modules.healthmetric.mapper.HealthMetricMapper;
import com.diabetes.assistant.modules.interventionreview.entity.InterventionReview;
import com.diabetes.assistant.modules.interventionreview.mapper.InterventionReviewMapper;
import com.diabetes.assistant.modules.lifeplan.entity.LifePlan;
import com.diabetes.assistant.modules.lifeplan.mapper.LifePlanMapper;
import com.diabetes.assistant.modules.profile.entity.PatientProfile;
import com.diabetes.assistant.modules.profile.mapper.PatientProfileMapper;
import com.diabetes.assistant.modules.risk.entity.RiskAssessment;
import com.diabetes.assistant.modules.risk.mapper.RiskAssessmentMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleRecommendationServiceImpl implements ArticleRecommendationService {

    private static final String ARTICLE_PUBLISHED = "published";
    private static final String ENGINE_RULE = "patient_profile_plan_review_rule";
    private static final String ENGINE_DIFY = "dify_knowledge_enhanced";
    private static final Set<String> SCENARIOS = Set.of("home", "life_plan", "intervention_review", "article_detail");

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleRecommendationLogMapper recommendationLogMapper;
    private final ArticleReadEventMapper readEventMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final HealthMetricMapper healthMetricMapper;
    private final RiskAssessmentMapper riskAssessmentMapper;
    private final LifePlanMapper lifePlanMapper;
    private final InterventionReviewMapper interventionReviewMapper;
    private final DifyService difyService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public List<ArticleRecommendationResponse> recommend(Integer userId, String scenario, Integer limit) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        String safeScenario = normalizeScenario(scenario);
        int safeLimit = limit == null || limit < 1 ? 6 : Math.min(limit, 12);
        EducationContext context = buildContext(userId);
        List<Article> articles = listPublishedArticles();
        if (articles.isEmpty()) {
            return List.of();
        }

        Map<Integer, List<ArticleTag>> tagMap = loadTags(articles);
        String batchKey = "edu-" + userId + "-" + safeScenario + "-" + UUID.randomUUID().toString().substring(0, 8);
        Map<Integer, DifyHint> difyHints = callDifyHints(context, articles, tagMap, safeScenario);

        List<ScoredArticle> scored = articles.stream()
                .map(article -> scoreArticle(article, tagMap.getOrDefault(article.getArticleId(), List.of()), context, safeScenario, difyHints.get(article.getArticleId())))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingInt(ScoredArticle::score).reversed()
                        .thenComparing(item -> item.article().getSortOrder() == null ? 999 : item.article().getSortOrder())
                        .thenComparing(item -> item.article().getArticleId() == null ? 0 : item.article().getArticleId()))
                .limit(safeLimit)
                .toList();

        if (scored.isEmpty()) {
            scored = articles.stream()
                    .sorted(Comparator.comparing((Article article) -> article.getIsRecommended() == null ? 0 : article.getIsRecommended()).reversed()
                            .thenComparing(article -> article.getSortOrder() == null ? 999 : article.getSortOrder()))
                    .limit(safeLimit)
                    .map(article -> new ScoredArticle(article, 20, List.of("基础科普补全"), "先补齐糖尿病基础认知，帮助理解后续生活方案。", ENGINE_RULE, false))
                    .toList();
        }

        List<ArticleRecommendationResponse> responses = new ArrayList<>();
        int rank = 1;
        for (ScoredArticle item : scored) {
            ArticleRecommendationLog log = new ArticleRecommendationLog();
            log.setUserId(userId);
            log.setArticleId(item.article().getArticleId());
            log.setScenario(safeScenario);
            log.setRankNo(rank);
            log.setScore(item.score());
            log.setSourceSignals(toJson(item.signals()));
            log.setReasonText(item.reason());
            log.setEngineType(item.engineType());
            log.setBatchKey(batchKey);
            log.setCreateTime(LocalDateTime.now());
            recommendationLogMapper.insert(log);

            responses.add(ArticleRecommendationResponse.builder()
                    .recommendationId(log.getRecommendationId())
                    .batchKey(batchKey)
                    .scenario(safeScenario)
                    .rankNo(rank)
                    .score(item.score())
                    .reason(item.reason())
                    .sourceSignals(item.signals())
                    .engineType(item.engineType())
                    .knowledgeEnhanced(item.knowledgeEnhanced())
                    .article(toArticleResponse(item.article(), tagMap.getOrDefault(item.article().getArticleId(), List.of())))
                    .build());
            rank++;
        }
        return responses;
    }

    @Override
    public PatientEducationProfileResponse getPatientEducationProfile(Integer userId) {
        EducationContext context = buildContext(userId);
        return PatientEducationProfileResponse.builder()
                .riskLevel(context.riskLevel())
                .profileTags(context.profileTags())
                .metricTags(context.metricTags())
                .planTags(context.planTags())
                .reviewTags(context.reviewTags())
                .readTags(context.readTags())
                .summary(buildProfileSummary(context))
                .build();
    }

    @Override
    @Transactional
    public ArticleReadEventResponse recordReadEvent(Integer userId, ArticleReadEventRequest request) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (request == null || request.getArticleId() == null) {
            throw new BusinessException(400, "文章 ID 不能为空");
        }
        Article article = articleMapper.selectById(request.getArticleId());
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }

        ArticleReadEvent event = new ArticleReadEvent();
        event.setUserId(userId);
        event.setArticleId(request.getArticleId());
        String sourceScenario = normalizeScenario(request.getSourceScenario());
        Integer recommendationId = resolveRecommendationId(userId, request.getArticleId(), request.getRecommendationId(), sourceScenario);
        int readSeconds = clamp(request.getReadSeconds(), 0, 86400);
        int progressPercent = clamp(request.getProgressPercent(), 0, 100);

        ArticleReadEvent existing = findActiveReadEvent(userId, request.getArticleId(), recommendationId, sourceScenario);
        if (existing != null) {
            existing.setReadSeconds(Math.max(safeInt(existing.getReadSeconds()), readSeconds));
            existing.setProgressPercent(Math.max(safeInt(existing.getProgressPercent()), progressPercent));
            readEventMapper.updateById(existing);
            event = existing;
        } else {
            event.setRecommendationId(recommendationId);
            event.setSourceScenario(sourceScenario);
            event.setReadSeconds(readSeconds);
            event.setProgressPercent(progressPercent);
            event.setCreateTime(LocalDateTime.now());
            readEventMapper.insert(event);
        }

        return ArticleReadEventResponse.builder()
                .eventId(event.getEventId())
                .articleId(event.getArticleId())
                .sourceScenario(event.getSourceScenario())
                .tracked(true)
                .build();
    }

    private Integer resolveRecommendationId(Integer userId, Integer articleId, Integer requestRecommendationId, String sourceScenario) {
        if (requestRecommendationId != null) {
            ArticleRecommendationLog log = recommendationLogMapper.selectById(requestRecommendationId);
            if (log != null
                    && Objects.equals(log.getUserId(), userId)
                    && Objects.equals(log.getArticleId(), articleId)) {
                return requestRecommendationId;
            }
        }

        ArticleRecommendationLog latestLog = recommendationLogMapper.selectOne(new LambdaQueryWrapper<ArticleRecommendationLog>()
                .eq(ArticleRecommendationLog::getUserId, userId)
                .eq(ArticleRecommendationLog::getArticleId, articleId)
                .eq(ArticleRecommendationLog::getScenario, sourceScenario)
                .ge(ArticleRecommendationLog::getCreateTime, LocalDateTime.now().minusDays(7))
                .orderByDesc(ArticleRecommendationLog::getCreateTime)
                .orderByDesc(ArticleRecommendationLog::getRecommendationId)
                .last("LIMIT 1"));
        if (latestLog != null) {
            return latestLog.getRecommendationId();
        }

        latestLog = recommendationLogMapper.selectOne(new LambdaQueryWrapper<ArticleRecommendationLog>()
                .eq(ArticleRecommendationLog::getUserId, userId)
                .eq(ArticleRecommendationLog::getArticleId, articleId)
                .ge(ArticleRecommendationLog::getCreateTime, LocalDateTime.now().minusDays(7))
                .orderByDesc(ArticleRecommendationLog::getCreateTime)
                .orderByDesc(ArticleRecommendationLog::getRecommendationId)
                .last("LIMIT 1"));
        return latestLog == null ? null : latestLog.getRecommendationId();
    }

    private ArticleReadEvent findActiveReadEvent(Integer userId, Integer articleId, Integer recommendationId, String sourceScenario) {
        LambdaQueryWrapper<ArticleReadEvent> wrapper = new LambdaQueryWrapper<ArticleReadEvent>()
                .eq(ArticleReadEvent::getUserId, userId)
                .eq(ArticleReadEvent::getArticleId, articleId)
                .eq(ArticleReadEvent::getSourceScenario, sourceScenario);
        if (recommendationId == null) {
            wrapper.isNull(ArticleReadEvent::getRecommendationId);
        } else {
            wrapper.eq(ArticleReadEvent::getRecommendationId, recommendationId);
        }
        wrapper.ge(ArticleReadEvent::getCreateTime, LocalDateTime.now().minusHours(2))
                .orderByDesc(ArticleReadEvent::getCreateTime)
                .orderByDesc(ArticleReadEvent::getEventId)
                .last("LIMIT 1");
        return readEventMapper.selectOne(wrapper);
    }

    private EducationContext buildContext(Integer userId) {
        PatientProfile profile = patientProfileMapper.selectOne(new LambdaQueryWrapper<PatientProfile>()
                .eq(PatientProfile::getUserId, userId)
                .last("LIMIT 1"));
        HealthMetric metric = healthMetricMapper.selectOne(new LambdaQueryWrapper<HealthMetric>()
                .eq(HealthMetric::getUserId, userId)
                .orderByDesc(HealthMetric::getRecordedAt)
                .orderByDesc(HealthMetric::getMetricId)
                .last("LIMIT 1"));
        RiskAssessment risk = riskAssessmentMapper.selectOne(new LambdaQueryWrapper<RiskAssessment>()
                .eq(RiskAssessment::getUserId, userId)
                .eq(RiskAssessment::getCallStatus, "success")
                .orderByDesc(RiskAssessment::getCreateTime)
                .orderByDesc(RiskAssessment::getAssessmentId)
                .last("LIMIT 1"));
        LifePlan plan = lifePlanMapper.selectOne(new LambdaQueryWrapper<LifePlan>()
                .eq(LifePlan::getUserId, userId)
                .eq(LifePlan::getCallStatus, "success")
                .orderByDesc(LifePlan::getUpdateTime)
                .orderByDesc(LifePlan::getPlanId)
                .last("LIMIT 1"));
        InterventionReview review = interventionReviewMapper.selectOne(new LambdaQueryWrapper<InterventionReview>()
                .eq(InterventionReview::getUserId, userId)
                .eq(InterventionReview::getCallStatus, "success")
                .orderByDesc(InterventionReview::getCreateTime)
                .orderByDesc(InterventionReview::getReviewId)
                .last("LIMIT 1"));

        List<String> profileTags = buildProfileTags(profile, metric, risk);
        List<String> metricTags = buildMetricTags(metric);
        List<String> planTags = buildPlanTags(plan);
        List<String> reviewTags = buildReviewTags(review);
        List<String> readTags = buildReadTags(userId);
        return new EducationContext(userId, profile, metric, risk, plan, review,
                risk == null ? "unknown" : text(risk.getRiskLevel(), "unknown"),
                profileTags, metricTags, planTags, reviewTags, readTags);
    }

    private List<String> buildProfileTags(PatientProfile profile, HealthMetric metric, RiskAssessment risk) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (profile != null) {
            if (profile.getAge() != null && profile.getAge() >= 60) tags.add("elder_safe_exercise");
            if (containsAny(profile.getFamilyHistory(), "糖尿病", "家族", "遗传")) tags.add("family_history");
            if (containsAny(profile.getChronicHistory(), "高血压", "血压")) tags.add("blood_pressure");
            if (containsAny(profile.getChronicHistory(), "血脂", "冠心病", "心血管")) tags.add("complication");
        }
        if (metric != null && metric.getWeightKg() != null && metric.getWaistCm() != null) {
            tags.add("weight_management");
        }
        if (risk != null && containsAny(risk.getMainRiskFactors(), "肥胖", "BMI", "腰围", "体重")) {
            tags.add("weight_management");
        }
        return new ArrayList<>(tags);
    }

    private List<String> buildMetricTags(HealthMetric metric) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (metric == null) {
            tags.add("basic_science");
            return new ArrayList<>(tags);
        }
        if (greater(metric.getFastingGlucose(), "6.1")) tags.add("fasting_glucose");
        if (greater(metric.getPostprandialGlucose(), "7.8")) tags.add("postprandial_glucose");
        if (greater(metric.getHba1c(), "6.0")) tags.add("glucose_monitoring");
        if ((metric.getSystolicBp() != null && metric.getSystolicBp() >= 130)
                || (metric.getDiastolicBp() != null && metric.getDiastolicBp() >= 85)) {
            tags.add("blood_pressure");
        }
        if (containsAny(metric.getDietStatus(), "外食", "主食", "甜", "饮料", "夜宵")) tags.add("diet_control");
        if (containsAny(metric.getExerciseStatus(), "少", "久坐", "没有", "不足")) tags.add("exercise_adherence_low");
        if (tags.isEmpty()) tags.add("habit_record");
        return new ArrayList<>(tags);
    }

    private List<String> buildPlanTags(LifePlan plan) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (plan == null) return List.of();
        String text = String.join("\n",
                text(plan.getPlanGoal(), ""),
                text(plan.getSummary(), ""),
                text(plan.getPlanJson(), ""),
                text(plan.getCheckinTasksJson(), ""));
        if (containsAny(text, "餐后", "散步", "步行")) tags.add("post_meal_walk");
        if (containsAny(text, "主食", "全谷", "杂豆", "餐盘")) tags.add("diet_control");
        if (containsAny(text, "运动", "有氧", "抗阻", "拉伸")) tags.add("exercise_plan");
        if (containsAny(text, "记录", "监测", "血糖")) tags.add("glucose_monitoring");
        if (containsAny(text, "睡眠", "作息", "压力")) tags.add("sleep_stress");
        return new ArrayList<>(tags);
    }

    private List<String> buildReviewTags(InterventionReview review) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (review == null) return List.of();
        addJsonOrTextTags(tags, review.getMainProblemTags());
        addJsonOrTextTags(tags, review.getChangedItems());
        String text = String.join("\n",
                text(review.getPatientNotice(), ""),
                text(review.getExplanation(), ""),
                text(review.getAdjustmentStrategy(), ""),
                text(review.getSafetyWarning(), ""));
        if (containsAny(text, "运动", "步行", "散步")) tags.add("exercise_adherence_low");
        if (containsAny(text, "饮食", "主食", "晚餐")) tags.add("diet_control");
        if (containsAny(text, "安全", "疼痛", "骨折", "不适")) tags.add("exercise_safety");
        return new ArrayList<>(tags);
    }

    private List<String> buildReadTags(Integer userId) {
        List<ArticleReadEvent> events = readEventMapper.selectList(new LambdaQueryWrapper<ArticleReadEvent>()
                .eq(ArticleReadEvent::getUserId, userId)
                .orderByDesc(ArticleReadEvent::getCreateTime)
                .last("LIMIT 20"));
        if (events.isEmpty()) return List.of();
        Map<Integer, Long> counts = events.stream()
                .collect(Collectors.groupingBy(ArticleReadEvent::getArticleId, LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> "read_article_" + entry.getKey())
                .toList();
    }

    private ScoredArticle scoreArticle(Article article, List<ArticleTag> tags, EducationContext context, String scenario, DifyHint hint) {
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        int score = article.getIsRecommended() != null && article.getIsRecommended() == 1 ? 8 : 0;
        score += Math.max(0, 10 - Math.min(article.getSortOrder() == null ? 10 : article.getSortOrder() / 100, 10));

        Set<String> articleSignals = new LinkedHashSet<>();
        articleSignals.add(article.getCategory());
        for (ArticleTag tag : tags) {
            articleSignals.add(tag.getTagCode());
            articleSignals.add(tag.getTagName());
        }
        articleSignals.addAll(inferSignalsFromArticle(article));

        score += matchSignals(articleSignals, context.profileTags(), 12, signals, "患者画像");
        score += matchSignals(articleSignals, context.metricTags(), 16, signals, "近期指标");
        score += matchSignals(articleSignals, context.planTags(), "life_plan".equals(scenario) ? 22 : 18, signals, "生活方案任务");
        score += matchSignals(articleSignals, context.reviewTags(), "intervention_review".equals(scenario) ? 26 : 18, signals, "自动复盘纠偏");
        if ("high".equalsIgnoreCase(context.riskLevel()) || "medium".equalsIgnoreCase(context.riskLevel())) {
            int riskScore = matchSignals(articleSignals, List.of("complication", "glucose_monitoring", "diet_control"), 8, signals, "风险分层");
            score += riskScore;
        }
        if (hint != null) {
            score += hint.scoreBoost();
            signals.add("知识库增强：" + hint.knowledgeSignal());
        }

        if (hasRecentRead(context.userId(), article.getArticleId())) {
            score -= 12;
            signals.add("近期已读降权");
        }

        String reason = hint != null && StringUtils.hasText(hint.reason())
                ? hint.reason()
                : buildReason(context, scenario, article, new ArrayList<>(signals));
        String engineType = hint != null ? ENGINE_DIFY : ENGINE_RULE;
        return new ScoredArticle(article, Math.max(score, 0), new ArrayList<>(signals), reason, engineType, hint != null);
    }

    private int matchSignals(Set<String> articleSignals, List<String> patientSignals, int weight, Set<String> matched, String source) {
        int score = 0;
        for (String signal : patientSignals) {
            if (!StringUtils.hasText(signal)) continue;
            if (articleSignals.stream().anyMatch(item -> equivalentSignal(item, signal))) {
                score += weight;
                matched.add(source + "：" + readableSignal(signal));
            }
        }
        return score;
    }

    private List<String> inferSignalsFromArticle(Article article) {
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        String text = String.join("\n", text(article.getTitle(), ""), text(article.getSummary(), ""), text(article.getContent(), ""), text(article.getCategory(), ""));
        if (containsAny(text, "餐盘", "主食", "饮食", "外食", "水果", "加餐")) signals.add("diet_control");
        if (containsAny(text, "餐后", "散步", "步行")) signals.add("post_meal_walk");
        if (containsAny(text, "运动", "有氧", "抗阻", "久坐", "鞋袜")) signals.add("exercise_plan");
        if (containsAny(text, "血糖", "空腹", "餐后", "糖化", "记录")) signals.add("glucose_monitoring");
        if (containsAny(text, "血压", "血脂", "眼", "足", "肾", "心血管", "并发症")) signals.add("complication");
        if (containsAny(text, "作息", "睡眠", "压力", "情绪", "饮水")) signals.add("sleep_stress");
        if (containsAny(text, "误区", "无糖", "保健品", "停药")) signals.add("mistake");
        if (containsAny(text, "BMI", "腰围", "体重", "肥胖")) signals.add("weight_management");
        if (containsAny(text, "高龄", "慢病", "安全", "低血糖")) signals.add("exercise_safety");
        return new ArrayList<>(signals);
    }

    private Map<Integer, DifyHint> callDifyHints(EducationContext context, List<Article> articles, Map<Integer, List<ArticleTag>> tagMap, String scenario) {
        try {
            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("scenario", scenario);
            inputs.put("patient_profile", toJson(getPatientEducationProfile(context.userId())));
            inputs.put("latest_plan_summary", context.plan() == null ? "" : context.plan().getSummary());
            inputs.put("latest_review_summary", context.review() == null ? "" : context.review().getExplanation());
            inputs.put("candidate_articles", toJson(articles.stream().limit(12).map(article -> Map.of(
                    "article_id", article.getArticleId(),
                    "title", text(article.getTitle(), ""),
                    "category", text(article.getCategory(), ""),
                    "summary", text(article.getSummary(), ""),
                    "tags", tagMap.getOrDefault(article.getArticleId(), List.of()).stream()
                            .map(tag -> text(tag.getTagName(), tag.getTagCode()))
                            .toList()
            )).toList()));
            DifyWorkflowResult result = difyService.callContentRecommendation(inputs, "user-" + context.userId());
            return parseDifyHints(result.getOutputs());
        } catch (Exception exception) {
            log.warn("Content recommendation Dify workflow failed, fallback to local rule engine: {}", exception.getMessage());
            return Map.of();
        }
    }

    private Map<Integer, DifyHint> parseDifyHints(Map<String, Object> outputs) {
        if (outputs == null || outputs.isEmpty()) return Map.of();
        Object value = outputs.getOrDefault("recommendations", outputs.get("recommendation_result"));
        JsonNode root = toJsonNode(value);
        if (root == null) return Map.of();
        JsonNode list = root.isArray() ? root : root.path("recommendations");
        if (!list.isArray()) return Map.of();
        Map<Integer, DifyHint> hints = new LinkedHashMap<>();
        for (JsonNode node : list) {
            int articleId = node.path("article_id").asInt(0);
            if (articleId <= 0) continue;
            hints.put(articleId, new DifyHint(
                    node.path("reason").asText(""),
                    node.path("knowledge_signal").asText("糖尿病知识库依据"),
                    node.path("score_boost").asInt(10)
            ));
        }
        return hints;
    }

    private JsonNode toJsonNode(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof String text) {
                return objectMapper.readTree(text);
            }
            return objectMapper.valueToTree(value);
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean hasRecentRead(Integer userId, Integer articleId) {
        if (userId == null || articleId == null) return false;
        Long count = readEventMapper.selectCount(new LambdaQueryWrapper<ArticleReadEvent>()
                .eq(ArticleReadEvent::getUserId, userId)
                .eq(ArticleReadEvent::getArticleId, articleId)
                .ge(ArticleReadEvent::getCreateTime, LocalDateTime.now().minusDays(7)));
        return count != null && count > 0;
    }

    private List<Article> listPublishedArticles() {
        return articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, ARTICLE_PUBLISHED)
                .orderByAsc(Article::getSortOrder)
                .orderByDesc(Article::getUpdateTime)
                .orderByDesc(Article::getArticleId));
    }

    private Map<Integer, List<ArticleTag>> loadTags(List<Article> articles) {
        List<Integer> ids = articles.stream().map(Article::getArticleId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) return Map.of();
        List<ArticleTag> tags = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()
                .in(ArticleTag::getArticleId, ids)
                .orderByDesc(ArticleTag::getWeight)
                .orderByAsc(ArticleTag::getTagId));
        return tags.stream().collect(Collectors.groupingBy(ArticleTag::getArticleId, LinkedHashMap::new, Collectors.toList()));
    }

    private ArticleResponse toArticleResponse(Article article, List<ArticleTag> tags) {
        return ArticleResponse.builder()
                .articleId(article.getArticleId())
                .title(article.getTitle())
                .category(article.getCategory())
                .coverImage(article.getCoverImage())
                .summary(article.getSummary())
                .content(null)
                .status(article.getStatus())
                .viewCount(article.getViewCount())
                .isRecommended(article.getIsRecommended())
                .sortOrder(article.getSortOrder())
                .createTime(DateTimeUtil.format(article.getCreateTime()))
                .updateTime(DateTimeUtil.format(article.getUpdateTime()))
                .tags(tags.stream().map(tag -> text(tag.getTagName(), tag.getTagCode())).filter(StringUtils::hasText).distinct().toList())
                .build();
    }

    private String buildReason(EducationContext context, String scenario, Article article, List<String> signals) {
        if ("life_plan".equals(scenario) && !context.planTags().isEmpty()) {
            return "当前生活方案包含" + readableSignal(context.planTags().get(0)) + "相关任务，这篇内容可作为执行前的配套科普，帮助你把建议转成具体动作。";
        }
        if ("intervention_review".equals(scenario) && !context.reviewTags().isEmpty()) {
            return "自动复盘识别到" + readableSignal(context.reviewTags().get(0)) + "，推荐这篇内容用于复盘后的行为纠偏和安全提醒。";
        }
        if (!context.metricTags().isEmpty()) {
            return "结合近期指标中的" + readableSignal(context.metricTags().get(0)) + "信号，系统推荐这篇科普帮助你理解指标变化和日常管理重点。";
        }
        if (!signals.isEmpty()) {
            return "系统根据" + signals.get(0) + "匹配到这篇文章，可补充当前阶段的控糖知识。";
        }
        return "这篇文章适合作为当前阶段的基础控糖科普阅读。";
    }

    private String buildProfileSummary(EducationContext context) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(context.riskLevel()) && !"unknown".equals(context.riskLevel())) {
            parts.add("风险分层：" + readableSignal(context.riskLevel()));
        }
        if (!context.metricTags().isEmpty()) {
            parts.add("近期指标：" + context.metricTags().stream().map(this::readableSignal).limit(2).collect(Collectors.joining("、")));
        }
        if (!context.planTags().isEmpty()) {
            parts.add("方案任务：" + context.planTags().stream().map(this::readableSignal).limit(2).collect(Collectors.joining("、")));
        }
        if (!context.reviewTags().isEmpty()) {
            parts.add("复盘关注：" + context.reviewTags().stream().map(this::readableSignal).limit(2).collect(Collectors.joining("、")));
        }
        return parts.isEmpty() ? "当前画像以基础糖尿病科普和健康行为建立为主。" : String.join("；", parts);
    }

    private boolean equivalentSignal(String left, String right) {
        String a = normalizeSignal(left);
        String b = normalizeSignal(right);
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) return false;
        if (a.equals(b) || a.contains(b) || b.contains(a)) return true;
        Map<String, Set<String>> groups = Map.of(
                "diet_control", Set.of("diet", "主食", "饮食", "控糖餐盘", "diet_control"),
                "post_meal_walk", Set.of("exercise", "餐后散步", "步行", "post_meal_walk"),
                "exercise_plan", Set.of("exercise", "运动", "有氧", "抗阻", "exercise_adherence_low"),
                "glucose_monitoring", Set.of("science", "血糖", "指标", "fasting_glucose", "postprandial_glucose"),
                "complication", Set.of("complication", "并发症", "血压", "眼", "足", "肾"),
                "sleep_stress", Set.of("habit", "作息", "睡眠", "压力"),
                "weight_management", Set.of("bmi", "体重", "腰围", "肥胖", "weight_management")
        );
        return groups.values().stream().anyMatch(group -> group.contains(a) && group.contains(b));
    }

    private String normalizeSignal(String value) {
        return text(value, "").trim().toLowerCase(Locale.ROOT).replace(" ", "_");
    }

    private String readableSignal(String signal) {
        String key = normalizeSignal(signal);
        return switch (key) {
            case "high" -> "高风险";
            case "medium" -> "中风险";
            case "low" -> "低风险";
            case "diet", "diet_control" -> "饮食结构优化";
            case "post_meal_walk" -> "餐后步行";
            case "exercise", "exercise_plan" -> "运动计划";
            case "exercise_adherence_low" -> "运动执行不足";
            case "exercise_safety" -> "运动安全";
            case "fasting_glucose" -> "空腹血糖偏高";
            case "postprandial_glucose" -> "餐后血糖偏高";
            case "glucose_monitoring" -> "血糖监测";
            case "blood_pressure" -> "血压管理";
            case "complication" -> "并发症预防";
            case "sleep_stress" -> "作息与压力管理";
            case "weight_management" -> "体重和腰围管理";
            case "family_history" -> "家族史风险";
            case "mistake" -> "控糖误区";
            default -> text(signal, "").replace("_", " ");
        };
    }

    private void addJsonOrTextTags(Set<String> tags, String value) {
        if (!StringUtils.hasText(value)) return;
        JsonNode node = toJsonNode(value);
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                String text = item.asText("");
                if (StringUtils.hasText(text)) tags.add(text);
            });
            return;
        }
        for (String part : value.split("[,，;；、\\s]+")) {
            if (StringUtils.hasText(part)) tags.add(part.trim());
        }
    }

    private String normalizeScenario(String scenario) {
        String normalized = text(scenario, "home").trim().toLowerCase(Locale.ROOT);
        return SCENARIOS.contains(normalized) ? normalized : "home";
    }

    private boolean containsAny(String value, String... keywords) {
        if (!StringUtils.hasText(value)) return false;
        for (String keyword : keywords) {
            if (value.contains(keyword)) return true;
        }
        return false;
    }

    private boolean greater(BigDecimal value, String threshold) {
        return value != null && value.compareTo(new BigDecimal(threshold)) > 0;
    }

    private int clamp(Integer value, int min, int max) {
        if (value == null) return 0;
        return Math.max(min, Math.min(max, value));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String text(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    private record EducationContext(
            Integer userId,
            PatientProfile profile,
            HealthMetric metric,
            RiskAssessment risk,
            LifePlan plan,
            InterventionReview review,
            String riskLevel,
            List<String> profileTags,
            List<String> metricTags,
            List<String> planTags,
            List<String> reviewTags,
            List<String> readTags) {
    }

    private record ScoredArticle(
            Article article,
            int score,
            List<String> signals,
            String reason,
            String engineType,
            boolean knowledgeEnhanced) {
    }

    private record DifyHint(String reason, String knowledgeSignal, int scoreBoost) {
    }
}
