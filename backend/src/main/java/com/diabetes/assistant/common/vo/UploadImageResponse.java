package com.diabetes.assistant.common.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadImageResponse {

    private String url;

    private String path;

    private String filename;

    @JsonProperty("original_filename")
    private String originalFilename;

    @JsonProperty("content_type")
    private String contentType;

    private Long size;
}
