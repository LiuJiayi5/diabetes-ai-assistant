package com.diabetes.assistant.modules.content.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HomeResponse {

    private List<HomeContentResponse> banners;
    @JsonProperty("ai_doctor_cards")
    private List<HomeContentResponse> aiDoctorCards;
    @JsonProperty("recommended_articles")
    private List<ArticleResponse> recommendedArticles;
}
