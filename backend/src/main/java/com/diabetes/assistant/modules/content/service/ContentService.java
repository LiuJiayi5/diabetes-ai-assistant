package com.diabetes.assistant.modules.content.service;

import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.content.dto.SaveArticleRequest;
import com.diabetes.assistant.modules.content.dto.SaveHomeContentRequest;
import com.diabetes.assistant.modules.content.vo.AdminContentManagementResponse;
import com.diabetes.assistant.modules.content.vo.ArticleResponse;
import com.diabetes.assistant.modules.content.vo.HomeContentResponse;
import com.diabetes.assistant.modules.content.vo.HomeResponse;

public interface ContentService {

    String entry();

    HomeResponse getHome();

    PageResult<ArticleResponse> listPublishedArticles(Integer page, Integer pageSize, String category, String keyword);

    ArticleResponse getPublishedArticleDetail(Integer articleId);

    AdminContentManagementResponse getAdminContentManagement(Integer adminUserId, Integer page, Integer pageSize,
                                                             String keyword, String articleStatus, String category,
                                                             String homeContentType, String homeStatus);

    ArticleResponse saveArticle(Integer adminUserId, SaveArticleRequest request);

    HomeContentResponse saveHomeContent(Integer adminUserId, SaveHomeContentRequest request);

    void deleteArticle(Integer adminUserId, Integer articleId);

    void deleteHomeContent(Integer adminUserId, Integer contentId);
}
