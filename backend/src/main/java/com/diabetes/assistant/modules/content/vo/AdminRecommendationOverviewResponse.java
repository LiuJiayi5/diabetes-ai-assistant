package com.diabetes.assistant.modules.content.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminRecommendationOverviewResponse {

    @JsonProperty("total_recommendations")
    private Long totalRecommendations;

    @JsonProperty("total_reads")
    private Long totalReads;

    @JsonProperty("avg_progress_percent")
    private Integer avgProgressPercent;

    @JsonProperty("avg_read_seconds")
    private Integer avgReadSeconds;

    @JsonProperty("knowledge_enhanced_count")
    private Long knowledgeEnhancedCount;

    @JsonProperty("knowledge_enhanced_rate")
    private Integer knowledgeEnhancedRate;

    @JsonProperty("scenario_stats")
    private List<ScenarioStat> scenarioStats;

    @JsonProperty("top_articles")
    private List<TopArticleStat> topArticles;

    @Data
    @Builder
    public static class ScenarioStat {
        private String scenario;
        private String label;
        @JsonProperty("recommendation_count")
        private Long recommendationCount;
        @JsonProperty("read_count")
        private Long readCount;
    }

    @Data
    @Builder
    public static class TopArticleStat {
        @JsonProperty("article_id")
        private Integer articleId;
        private String title;
        @JsonProperty("read_count")
        private Long readCount;
        @JsonProperty("recommendation_count")
        private Long recommendationCount;
    }
}
