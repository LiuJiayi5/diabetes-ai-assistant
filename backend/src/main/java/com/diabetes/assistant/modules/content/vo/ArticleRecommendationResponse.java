package com.diabetes.assistant.modules.content.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ArticleRecommendationResponse {

    @JsonProperty("recommendation_id")
    private Integer recommendationId;

    @JsonProperty("batch_key")
    private String batchKey;

    @JsonProperty("scenario")
    private String scenario;

    @JsonProperty("rank_no")
    private Integer rankNo;

    @JsonProperty("score")
    private Integer score;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("source_signals")
    private List<String> sourceSignals;

    @JsonProperty("engine_type")
    private String engineType;

    @JsonProperty("knowledge_enhanced")
    private Boolean knowledgeEnhanced;

    @JsonProperty("article")
    private ArticleResponse article;
}
