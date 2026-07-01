package com.diabetes.assistant.modules.content.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ArticleReadEventRequest {

    @JsonProperty("article_id")
    private Integer articleId;

    @JsonProperty("recommendation_id")
    private Integer recommendationId;

    @JsonProperty("source_scenario")
    private String sourceScenario;

    @JsonProperty("read_seconds")
    private Integer readSeconds;

    @JsonProperty("progress_percent")
    private Integer progressPercent;
}
