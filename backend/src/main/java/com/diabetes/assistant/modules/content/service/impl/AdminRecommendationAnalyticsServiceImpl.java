package com.diabetes.assistant.modules.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.DateTimeUtil;
import com.diabetes.assistant.modules.content.entity.Article;
import com.diabetes.assistant.modules.content.entity.ArticleReadEvent;
import com.diabetes.assistant.modules.content.entity.ArticleRecommendationLog;
import com.diabetes.assistant.modules.content.mapper.ArticleMapper;
import com.diabetes.assistant.modules.content.mapper.ArticleReadEventMapper;
import com.diabetes.assistant.modules.content.mapper.ArticleRecommendationLogMapper;
import com.diabetes.assistant.modules.content.service.AdminRecommendationAnalyticsService;
import com.diabetes.assistant.modules.content.vo.AdminReadEventResponse;
import com.diabetes.assistant.modules.content.vo.AdminRecommendationDashboardResponse;
import com.diabetes.assistant.modules.content.vo.AdminRecommendationLogResponse;
import com.diabetes.assistant.modules.content.vo.AdminRecommendationOverviewResponse;
import com.diabetes.assistant.modules.user.entity.User;
import com.diabetes.assistant.modules.user.mapper.UserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminRecommendationAnalyticsServiceImpl implements AdminRecommendationAnalyticsService {

    private static final String ADMIN = "admin";
    private static final String ENGINE_DIFY = "dify_knowledge_enhanced";
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final ArticleRecommendationLogMapper recommendationLogMapper;
    private final ArticleReadEventMapper readEventMapper;
    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AdminRecommendationDashboardResponse getDashboard(Integer adminUserId,
                                                             Integer page,
                                                             Integer pageSize,
                                                             String scenario,
                                                             String keyword,
                                                             Boolean knowledgeEnhanced,
                                                             String readStatus) {
        requireAdmin(adminUserId);
        int currentPage = page == null || page < 1 ? 1 : page;
        int currentPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);

        List<ArticleRecommendationLog> allLogs = recommendationLogMapper.selectList(new LambdaQueryWrapper<ArticleRecommendationLog>()
                .orderByDesc(ArticleRecommendationLog::getCreateTime)
                .orderByDesc(ArticleRecommendationLog::getRecommendationId));
        List<ArticleReadEvent> rawReads = readEventMapper.selectList(new LambdaQueryWrapper<ArticleReadEvent>()
                .orderByDesc(ArticleReadEvent::getCreateTime)
                .orderByDesc(ArticleReadEvent::getEventId));
        List<ArticleReadEvent> allReads = compactReadSessions(rawReads);

        Map<Integer, User> users = loadUsers(allLogs, allReads);
        Map<Integer, Article> articles = loadArticles(allLogs, allReads);
        Map<Integer, List<ArticleReadEvent>> readsByRecommendation = readsByRecommendation(allReads);
        List<ArticleRecommendationLog> filteredLogs = filterLogs(allLogs, users, articles, scenario, keyword, knowledgeEnhanced, readStatus, readsByRecommendation);
        List<ArticleReadEvent> filteredReads = filterReads(allReads, users, articles, scenario, keyword);

        PageResult<AdminRecommendationLogResponse> recommendationPage = pageLogs(filteredLogs, allReads, readsByRecommendation, users, articles, currentPage, currentPageSize);
        PageResult<AdminReadEventResponse> readPage = pageReads(filteredReads, users, articles, currentPage, Math.min(currentPageSize, 20));

        return AdminRecommendationDashboardResponse.builder()
                .overview(buildOverview(filteredLogs, filteredReads, articles))
                .recommendations(recommendationPage)
                .readEvents(readPage)
                .build();
    }

    private List<ArticleReadEvent> compactReadSessions(List<ArticleReadEvent> reads) {
        List<ArticleReadEvent> sessions = new ArrayList<>();
        for (ArticleReadEvent event : reads) {
            ArticleReadEvent active = sessions.stream()
                    .filter(candidate -> sameReadSession(candidate, event))
                    .findFirst()
                    .orElse(null);
            if (active == null) {
                sessions.add(copyReadEvent(event));
                continue;
            }
            active.setReadSeconds(Math.max(safeInt(active.getReadSeconds()), safeInt(event.getReadSeconds())));
            active.setProgressPercent(Math.max(safeInt(active.getProgressPercent()), safeInt(event.getProgressPercent())));
            active.setRecommendationId(preferredRecommendationId(active.getRecommendationId(), event.getRecommendationId()));
            active.setSourceScenario(preferredScenario(active.getSourceScenario(), event.getSourceScenario()));
            if (event.getCreateTime() != null
                    && (active.getCreateTime() == null || event.getCreateTime().isAfter(active.getCreateTime()))) {
                active.setCreateTime(event.getCreateTime());
                active.setEventId(event.getEventId());
            }
        }
        sessions.sort(Comparator.comparing(ArticleReadEvent::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(ArticleReadEvent::getEventId, Comparator.nullsLast(Comparator.reverseOrder())));
        return sessions;
    }

    private ArticleReadEvent copyReadEvent(ArticleReadEvent source) {
        ArticleReadEvent copy = new ArticleReadEvent();
        copy.setEventId(source.getEventId());
        copy.setUserId(source.getUserId());
        copy.setArticleId(source.getArticleId());
        copy.setRecommendationId(source.getRecommendationId());
        copy.setSourceScenario(source.getSourceScenario());
        copy.setReadSeconds(source.getReadSeconds());
        copy.setProgressPercent(source.getProgressPercent());
        copy.setCreateTime(source.getCreateTime());
        return copy;
    }

    private boolean sameReadSession(ArticleReadEvent left, ArticleReadEvent right) {
        if (!Objects.equals(left.getUserId(), right.getUserId())
                || !Objects.equals(left.getArticleId(), right.getArticleId())) {
            return false;
        }
        if (Objects.equals(left.getUserId(), right.getUserId())
                && Objects.equals(left.getArticleId(), right.getArticleId())
                && left.getRecommendationId() != null
                && Objects.equals(left.getRecommendationId(), right.getRecommendationId())) {
            return withinSameSession(left.getCreateTime(), right.getCreateTime(), 120);
        }
        if (Objects.equals(left.getRecommendationId(), right.getRecommendationId())
                && Objects.equals(text(left.getSourceScenario(), ""), text(right.getSourceScenario(), ""))) {
            return withinSameSession(left.getCreateTime(), right.getCreateTime(), 120);
        }
        if ((left.getRecommendationId() == null) != (right.getRecommendationId() == null)
                && ("article_detail".equals(left.getSourceScenario()) || "article_detail".equals(right.getSourceScenario()))) {
            return withinSameSession(left.getCreateTime(), right.getCreateTime(), 5);
        }
        return false;
    }

    private Integer preferredRecommendationId(Integer left, Integer right) {
        return left == null ? right : left;
    }

    private String preferredScenario(String left, String right) {
        if (!StringUtils.hasText(left)) return right;
        if (!StringUtils.hasText(right)) return left;
        if ("article_detail".equals(left) && !"article_detail".equals(right)) return right;
        return left;
    }

    private boolean withinSameSession(LocalDateTime left, LocalDateTime right, long minutes) {
        if (left == null || right == null) return true;
        return Math.abs(Duration.between(left, right).toMinutes()) <= minutes;
    }

    private void requireAdmin(Integer adminUserId) {
        if (adminUserId == null) {
            throw new BusinessException(401, "请先登录");
        }
        User user = userMapper.selectById(adminUserId);
        if (user == null || !ADMIN.equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(403, "Admin permission required");
        }
    }

    private List<ArticleRecommendationLog> filterLogs(List<ArticleRecommendationLog> logs,
                                                      Map<Integer, User> users,
                                                      Map<Integer, Article> articles,
                                                      String scenario,
                                                      String keyword,
                                                      Boolean knowledgeEnhanced,
                                                      String readStatus,
                                                      Map<Integer, List<ArticleReadEvent>> readsByRecommendation) {
        String normalizedScenario = normalize(scenario);
        String normalizedKeyword = normalize(keyword).toLowerCase(Locale.ROOT);
        String normalizedReadStatus = normalize(readStatus).toLowerCase(Locale.ROOT);
        return logs.stream()
                .filter(log -> !StringUtils.hasText(normalizedScenario) || normalizedScenario.equals(log.getScenario()))
                .filter(log -> knowledgeEnhanced == null || knowledgeEnhanced == isKnowledgeEnhanced(log))
                .filter(log -> matchesReadStatus(log, readsByRecommendation, normalizedReadStatus))
                .filter(log -> !StringUtils.hasText(normalizedKeyword) || matchesKeyword(log, users.get(log.getUserId()), articles.get(log.getArticleId()), normalizedKeyword))
                .toList();
    }

    private List<ArticleReadEvent> filterReads(List<ArticleReadEvent> reads,
                                               Map<Integer, User> users,
                                               Map<Integer, Article> articles,
                                               String scenario,
                                               String keyword) {
        String normalizedScenario = normalize(scenario);
        String normalizedKeyword = normalize(keyword).toLowerCase(Locale.ROOT);
        return reads.stream()
                .filter(event -> !StringUtils.hasText(normalizedScenario) || normalizedScenario.equals(event.getSourceScenario()))
                .filter(event -> !StringUtils.hasText(normalizedKeyword) || matchesReadKeyword(event, users.get(event.getUserId()), articles.get(event.getArticleId()), normalizedKeyword))
                .toList();
    }

    private Map<Integer, List<ArticleReadEvent>> readsByRecommendation(List<ArticleReadEvent> reads) {
        return reads.stream()
                .filter(event -> event.getRecommendationId() != null)
                .collect(Collectors.groupingBy(ArticleReadEvent::getRecommendationId));
    }

    private boolean matchesReadStatus(ArticleRecommendationLog log,
                                      Map<Integer, List<ArticleReadEvent>> readsByRecommendation,
                                      String readStatus) {
        if (!StringUtils.hasText(readStatus) || "all".equals(readStatus)) {
            return true;
        }
        boolean hasRead = !readsByRecommendation.getOrDefault(log.getRecommendationId(), List.of()).isEmpty();
        return "read".equals(readStatus) ? hasRead : "unread".equals(readStatus) ? !hasRead : true;
    }

    private AdminRecommendationOverviewResponse buildOverview(List<ArticleRecommendationLog> logs,
                                                              List<ArticleReadEvent> reads,
                                                              Map<Integer, Article> articles) {
        long enhanced = logs.stream().filter(this::isKnowledgeEnhanced).count();
        int enhancedRate = logs.isEmpty() ? 0 : Math.round(enhanced * 100f / logs.size());
        int avgProgress = reads.isEmpty() ? 0 : Math.round((float) reads.stream().mapToInt(event -> safeInt(event.getProgressPercent())).average().orElse(0));
        int avgSeconds = reads.isEmpty() ? 0 : Math.round((float) reads.stream().mapToInt(event -> safeInt(event.getReadSeconds())).average().orElse(0));

        Map<String, Long> logByScenario = logs.stream()
                .collect(Collectors.groupingBy(log -> text(log.getScenario(), "unknown"), LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> readByScenario = reads.stream()
                .collect(Collectors.groupingBy(read -> text(read.getSourceScenario(), "unknown"), LinkedHashMap::new, Collectors.counting()));
        List<String> scenarios = new ArrayList<>();
        scenarios.addAll(logByScenario.keySet());
        readByScenario.keySet().forEach(item -> {
            if (!scenarios.contains(item)) scenarios.add(item);
        });

        Map<Integer, Long> readCountByArticle = reads.stream()
                .collect(Collectors.groupingBy(ArticleReadEvent::getArticleId, Collectors.counting()));
        Map<Integer, Long> recommendationCountByArticle = logs.stream()
                .collect(Collectors.groupingBy(ArticleRecommendationLog::getArticleId, Collectors.counting()));

        return AdminRecommendationOverviewResponse.builder()
                .totalRecommendations((long) logs.size())
                .totalReads((long) reads.size())
                .avgProgressPercent(avgProgress)
                .avgReadSeconds(avgSeconds)
                .knowledgeEnhancedCount(enhanced)
                .knowledgeEnhancedRate(enhancedRate)
                .scenarioStats(scenarios.stream()
                        .map(scenario -> AdminRecommendationOverviewResponse.ScenarioStat.builder()
                                .scenario(scenario)
                                .label(scenarioLabel(scenario))
                                .recommendationCount(logByScenario.getOrDefault(scenario, 0L))
                                .readCount(readByScenario.getOrDefault(scenario, 0L))
                                .build())
                        .toList())
                .topArticles(readCountByArticle.entrySet().stream()
                        .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                        .limit(5)
                        .map(entry -> {
                            Article article = articles.get(entry.getKey());
                            return AdminRecommendationOverviewResponse.TopArticleStat.builder()
                                    .articleId(entry.getKey())
                                    .title(article == null ? "文章 #" + entry.getKey() : article.getTitle())
                                    .readCount(entry.getValue())
                                    .recommendationCount(recommendationCountByArticle.getOrDefault(entry.getKey(), 0L))
                                    .build();
                        })
                        .toList())
                .build();
    }

    private PageResult<AdminRecommendationLogResponse> pageLogs(List<ArticleRecommendationLog> logs,
                                                                List<ArticleReadEvent> reads,
                                                                Map<Integer, List<ArticleReadEvent>> readsByRecommendation,
                                                                Map<Integer, User> users,
                                                                Map<Integer, Article> articles,
                                                                int page,
                                                                int pageSize) {
        int from = Math.min((page - 1) * pageSize, logs.size());
        int to = Math.min(from + pageSize, logs.size());
        List<ArticleRecommendationLog> pageRows = logs.subList(from, to);

        List<AdminRecommendationLogResponse> rows = pageRows.stream()
                .map(log -> toLogResponse(log, readsForLog(log, reads, readsByRecommendation),
                        users.get(log.getUserId()), articles.get(log.getArticleId())))
                .toList();
        return new PageResult<>(rows, (long) logs.size(), page, pageSize);
    }

    private List<ArticleReadEvent> readsForLog(ArticleRecommendationLog log,
                                               List<ArticleReadEvent> reads,
                                               Map<Integer, List<ArticleReadEvent>> readsByRecommendation) {
        Map<Integer, ArticleReadEvent> matched = new LinkedHashMap<>();
        readsByRecommendation.getOrDefault(log.getRecommendationId(), List.of()).forEach(event -> matched.put(event.getEventId(), event));

        LocalDateTime start = log.getCreateTime();
        LocalDateTime end = start == null ? null : start.plusDays(7);
        reads.stream()
                .filter(event -> event.getRecommendationId() == null)
                .filter(event -> Objects.equals(event.getUserId(), log.getUserId()))
                .filter(event -> Objects.equals(event.getArticleId(), log.getArticleId()))
                .filter(event -> !StringUtils.hasText(log.getScenario()) || !StringUtils.hasText(event.getSourceScenario()) || Objects.equals(event.getSourceScenario(), log.getScenario()))
                .filter(event -> start == null || event.getCreateTime() == null || !event.getCreateTime().isBefore(start))
                .filter(event -> end == null || event.getCreateTime() == null || event.getCreateTime().isBefore(end))
                .forEach(event -> matched.put(event.getEventId(), event));
        return new ArrayList<>(matched.values());
    }

    private PageResult<AdminReadEventResponse> pageReads(List<ArticleReadEvent> reads,
                                                         Map<Integer, User> users,
                                                         Map<Integer, Article> articles,
                                                         int page,
                                                         int pageSize) {
        int from = Math.min((page - 1) * pageSize, reads.size());
        int to = Math.min(from + pageSize, reads.size());
        List<AdminReadEventResponse> rows = reads.subList(from, to).stream()
                .map(event -> toReadResponse(event, users.get(event.getUserId()), articles.get(event.getArticleId())))
                .toList();
        return new PageResult<>(rows, (long) reads.size(), page, pageSize);
    }

    private AdminRecommendationLogResponse toLogResponse(ArticleRecommendationLog log,
                                                         List<ArticleReadEvent> reads,
                                                         User user,
                                                         Article article) {
        int avgSeconds = reads.isEmpty() ? 0 : Math.round((float) reads.stream().mapToInt(event -> safeInt(event.getReadSeconds())).average().orElse(0));
        int avgProgress = reads.isEmpty() ? 0 : Math.round((float) reads.stream().mapToInt(event -> safeInt(event.getProgressPercent())).average().orElse(0));
        LocalDateTime latestRead = reads.stream()
                .map(ArticleReadEvent::getCreateTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return AdminRecommendationLogResponse.builder()
                .recommendationId(log.getRecommendationId())
                .batchKey(log.getBatchKey())
                .scenario(log.getScenario())
                .scenarioLabel(scenarioLabel(log.getScenario()))
                .rankNo(log.getRankNo())
                .score(log.getScore())
                .engineType(log.getEngineType())
                .knowledgeEnhanced(isKnowledgeEnhanced(log))
                .reason(log.getReasonText())
                .sourceSignals(parseSignals(log.getSourceSignals()))
                .createTime(DateTimeUtil.format(log.getCreateTime()))
                .readCount((long) reads.size())
                .avgReadSeconds(avgSeconds)
                .avgProgressPercent(avgProgress)
                .latestReadTime(DateTimeUtil.format(latestRead))
                .user(toUserBrief(user, log.getUserId()))
                .article(toArticleBrief(article, log.getArticleId()))
                .build();
    }

    private AdminReadEventResponse toReadResponse(ArticleReadEvent event, User user, Article article) {
        return AdminReadEventResponse.builder()
                .eventId(event.getEventId())
                .recommendationId(event.getRecommendationId())
                .sourceScenario(event.getSourceScenario())
                .sourceScenarioLabel(scenarioLabel(event.getSourceScenario()))
                .readSeconds(event.getReadSeconds())
                .progressPercent(event.getProgressPercent())
                .createTime(DateTimeUtil.format(event.getCreateTime()))
                .user(toUserBrief(user, event.getUserId()))
                .article(toArticleBrief(article, event.getArticleId()))
                .build();
    }

    private AdminRecommendationLogResponse.UserBrief toUserBrief(User user, Integer fallbackId) {
        return AdminRecommendationLogResponse.UserBrief.builder()
                .userId(user == null ? fallbackId : user.getUserId())
                .username(user == null ? "用户 #" + fallbackId : user.getUsername())
                .phone(user == null ? "" : user.getPhone())
                .avatar(user == null ? "" : user.getAvatar())
                .build();
    }

    private AdminRecommendationLogResponse.ArticleBrief toArticleBrief(Article article, Integer fallbackId) {
        return AdminRecommendationLogResponse.ArticleBrief.builder()
                .articleId(article == null ? fallbackId : article.getArticleId())
                .title(article == null ? "文章 #" + fallbackId : article.getTitle())
                .category(article == null ? "" : article.getCategory())
                .status(article == null ? "" : article.getStatus())
                .isRecommended(article == null ? 0 : article.getIsRecommended())
                .build();
    }

    private Map<Integer, User> loadUsers(List<ArticleRecommendationLog> logs, List<ArticleReadEvent> reads) {
        Set<Integer> ids = new java.util.LinkedHashSet<>();
        logs.stream().map(ArticleRecommendationLog::getUserId).filter(Objects::nonNull).forEach(ids::add);
        reads.stream().map(ArticleReadEvent::getUserId).filter(Objects::nonNull).forEach(ids::add);
        if (ids.isEmpty()) return Map.of();
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getUserId, ids)).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Integer, Article> loadArticles(List<ArticleRecommendationLog> logs, List<ArticleReadEvent> reads) {
        Set<Integer> ids = new java.util.LinkedHashSet<>();
        logs.stream().map(ArticleRecommendationLog::getArticleId).filter(Objects::nonNull).forEach(ids::add);
        reads.stream().map(ArticleReadEvent::getArticleId).filter(Objects::nonNull).forEach(ids::add);
        if (ids.isEmpty()) return Map.of();
        return articleMapper.selectList(new LambdaQueryWrapper<Article>().in(Article::getArticleId, ids)).stream()
                .collect(Collectors.toMap(Article::getArticleId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private boolean matchesKeyword(ArticleRecommendationLog log, User user, Article article, String keyword) {
        String text = String.join(" ",
                text(log.getReasonText(), ""),
                text(log.getBatchKey(), ""),
                user == null ? "" : text(user.getUsername(), ""),
                user == null ? "" : text(user.getPhone(), ""),
                article == null ? "" : text(article.getTitle(), ""));
        return text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean matchesReadKeyword(ArticleReadEvent event, User user, Article article, String keyword) {
        String text = String.join(" ",
                String.valueOf(event.getRecommendationId()),
                user == null ? "" : text(user.getUsername(), ""),
                user == null ? "" : text(user.getPhone(), ""),
                article == null ? "" : text(article.getTitle(), ""));
        return text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private List<String> parseSignals(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        try {
            List<String> parsed = objectMapper.readValue(value, STRING_LIST);
            return parsed == null ? List.of() : parsed;
        } catch (Exception exception) {
            return List.of(value);
        }
    }

    private boolean isKnowledgeEnhanced(ArticleRecommendationLog log) {
        return ENGINE_DIFY.equalsIgnoreCase(log.getEngineType());
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String text(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String scenarioLabel(String value) {
        return switch (text(value, "")) {
            case "home" -> "首页推荐";
            case "life_plan" -> "方案配套";
            case "intervention_review" -> "复盘更新";
            case "article_detail" -> "详情续读";
            default -> StringUtils.hasText(value) ? value : "未知来源";
        };
    }
}
