package com.diabetes.assistant.modules.content.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.content.dto.SaveArticleRequest;
import com.diabetes.assistant.modules.content.dto.SaveHomeContentRequest;
import com.diabetes.assistant.modules.content.service.ContentService;
import com.diabetes.assistant.modules.content.vo.AdminContentManagementResponse;
import com.diabetes.assistant.modules.content.vo.ArticleResponse;
import com.diabetes.assistant.modules.content.vo.HomeContentResponse;
import com.diabetes.assistant.modules.content.vo.HomeResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping("/api/content/entry")
    public ApiResponse<String> entry() {
        return ApiResponse.success(contentService.entry());
    }

    @GetMapping("/api/home")
    public ApiResponse<HomeResponse> getHome() {
        return ApiResponse.success(contentService.getHome());
    }

    @GetMapping("/api/articles")
    public ApiResponse<PageResult<ArticleResponse>> listArticles(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(contentService.listPublishedArticles(page, pageSize, category, keyword));
    }

    @GetMapping("/api/articles/{articleId}")
    public ApiResponse<ArticleResponse> getArticleDetail(@PathVariable Integer articleId) {
        return ApiResponse.success(contentService.getPublishedArticleDetail(articleId));
    }

    @GetMapping("/api/admin/content-management")
    public ApiResponse<AdminContentManagementResponse> getAdminContentManagement(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "article_status", required = false) String articleStatus,
            @RequestParam(required = false) String category,
            @RequestParam(name = "home_content_type", required = false) String homeContentType,
            @RequestParam(name = "home_status", required = false) String homeStatus) {
        Integer adminUserId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(contentService.getAdminContentManagement(adminUserId, page, pageSize, keyword,
                articleStatus, category, homeContentType, homeStatus));
    }

    @PostMapping("/api/admin/articles/save")
    public ApiResponse<ArticleResponse> saveArticle(HttpServletRequest request, @RequestBody SaveArticleRequest saveRequest) {
        Integer adminUserId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(contentService.saveArticle(adminUserId, saveRequest));
    }

    @PostMapping("/api/admin/home-contents/save")
    public ApiResponse<HomeContentResponse> saveHomeContent(HttpServletRequest request, @RequestBody SaveHomeContentRequest saveRequest) {
        Integer adminUserId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(contentService.saveHomeContent(adminUserId, saveRequest));
    }

    @DeleteMapping("/api/admin/articles/{articleId}")
    public ApiResponse<Void> deleteArticle(HttpServletRequest request, @PathVariable Integer articleId) {
        Integer adminUserId = currentUserUtil.getCurrentUserId(request);
        contentService.deleteArticle(adminUserId, articleId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/api/admin/home-contents/{contentId}")
    public ApiResponse<Void> deleteHomeContent(HttpServletRequest request, @PathVariable Integer contentId) {
        Integer adminUserId = currentUserUtil.getCurrentUserId(request);
        contentService.deleteHomeContent(adminUserId, contentId);
        return ApiResponse.success(null);
    }
}
