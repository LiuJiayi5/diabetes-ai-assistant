package com.diabetes.assistant.modules.content.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SaveHomeContentRequest {

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
}
