package com.diabetes.assistant.modules.content.contract.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleSummaryDTO {

    private Integer articleId;
    private String title;
    private String category;
    private String coverImage;
    private String summary;
    private Integer viewCount;
    private Boolean isRecommended;
    private LocalDateTime createTime;
}
