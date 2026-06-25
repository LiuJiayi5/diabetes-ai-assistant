package com.diabetes.assistant.modules.content.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomeContentResponse {

    @JsonProperty("content_id")
    private Integer contentId;
    @JsonProperty("content_type")
    private String contentType;
    private String title;
    private String subtitle;
    @JsonProperty("image_url")
    private String imageUrl;
    @JsonProperty("link_type")
    private String linkType;
    @JsonProperty("link_value")
    private String linkValue;
    @JsonProperty("sort_order")
    private Integer sortOrder;
    private String status;
    @JsonProperty("created_by")
    private Integer createdBy;
    @JsonProperty("create_time")
    private String createTime;
    @JsonProperty("update_time")
    private String updateTime;
}
