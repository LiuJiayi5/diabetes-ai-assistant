package com.diabetes.assistant.modules.content.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleResponse {

    @JsonProperty("article_id")
    private Integer articleId;
    private String title;
    private String category;
    @JsonProperty("cover_image")
    private String coverImage;
    private String summary;
    private String content;
    private String status;
    @JsonProperty("view_count")
    private Integer viewCount;
    @JsonProperty("is_recommended")
    private Integer isRecommended;
    @JsonProperty("sort_order")
    private Integer sortOrder;
    private List<String> tags;
    @JsonProperty("create_time")
    private String createTime;
    @JsonProperty("update_time")
    private String updateTime;
}
