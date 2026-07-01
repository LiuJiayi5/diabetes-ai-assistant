package com.diabetes.assistant.modules.content.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminRecommendationLogResponse {

    @JsonProperty("recommendation_id")
    private Integer recommendationId;

    @JsonProperty("batch_key")
    private String batchKey;

    private String scenario;

    @JsonProperty("scenario_label")
    private String scenarioLabel;

    @JsonProperty("rank_no")
    private Integer rankNo;

    private Integer score;

    @JsonProperty("engine_type")
    private String engineType;

    @JsonProperty("knowledge_enhanced")
    private Boolean knowledgeEnhanced;

    private String reason;

    @JsonProperty("source_signals")
    private List<String> sourceSignals;

    @JsonProperty("create_time")
    private String createTime;

    @JsonProperty("read_count")
    private Long readCount;

    @JsonProperty("avg_read_seconds")
    private Integer avgReadSeconds;

    @JsonProperty("avg_progress_percent")
    private Integer avgProgressPercent;

    @JsonProperty("latest_read_time")
    private String latestReadTime;

    private UserBrief user;

    private ArticleBrief article;

    @Data
    @Builder
    public static class UserBrief {
        @JsonProperty("user_id")
        private Integer userId;
        private String username;
        private String phone;
    }

    @Data
    @Builder
    public static class ArticleBrief {
        @JsonProperty("article_id")
        private Integer articleId;
        private String title;
        private String category;
        private String status;
        @JsonProperty("is_recommended")
        private Integer isRecommended;
    }
}
