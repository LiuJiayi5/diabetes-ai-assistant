package com.diabetes.assistant.modules.content.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SaveArticleRequest {

    @JsonProperty("article_id")
    private Integer articleId;
    private String title;
    private String category;
    @JsonProperty("cover_image")
    private String coverImage;
    private String summary;
    private String content;
    private String status;
    @JsonProperty("is_recommended")
    private Integer isRecommended;
    @JsonProperty("sort_order")
    private Integer sortOrder;
}
