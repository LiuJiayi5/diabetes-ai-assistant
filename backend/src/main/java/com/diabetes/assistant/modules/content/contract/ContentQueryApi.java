package com.diabetes.assistant.modules.content.contract;

import com.diabetes.assistant.modules.content.contract.dto.ArticleSummaryDTO;
import com.diabetes.assistant.modules.content.contract.dto.HomeContentDTO;

import java.util.List;

public interface ContentQueryApi {

    ArticleSummaryDTO getArticleSummaryById(Integer articleId);

    List<ArticleSummaryDTO> listRecommendedArticles(Integer limit);

    List<HomeContentDTO> listEnabledHomeContents();
}
