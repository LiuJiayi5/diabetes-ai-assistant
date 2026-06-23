package com.diabetes.assistant.modules.content.contract.dto;

import lombok.Data;

@Data
public class HomeContentDTO {

    private Integer contentId;
    private String contentType;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String linkType;
    private String linkValue;
    private Integer sortOrder;
    private String status;
}
