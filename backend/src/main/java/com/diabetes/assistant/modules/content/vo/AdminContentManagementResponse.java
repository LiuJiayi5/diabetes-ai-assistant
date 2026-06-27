package com.diabetes.assistant.modules.content.vo;

import com.diabetes.assistant.common.response.PageResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminContentManagementResponse {

    private PageResult<ArticleResponse> articles;
    @JsonProperty("home_contents")
    private List<HomeContentResponse> homeContents;
}
