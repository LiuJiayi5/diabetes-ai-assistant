package com.diabetes.assistant.modules.content.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArticleReadEventResponse {

    @JsonProperty("event_id")
    private Integer eventId;

    @JsonProperty("article_id")
    private Integer articleId;

    @JsonProperty("source_scenario")
    private String sourceScenario;

    @JsonProperty("tracked")
    private Boolean tracked;
}
