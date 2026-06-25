package com.diabetes.assistant.modules.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.DateTimeUtil;
import com.diabetes.assistant.modules.content.contract.ContentQueryApi;
import com.diabetes.assistant.modules.content.contract.dto.ArticleSummaryDTO;
import com.diabetes.assistant.modules.content.contract.dto.HomeContentDTO;
import com.diabetes.assistant.modules.content.dto.SaveArticleRequest;
import com.diabetes.assistant.modules.content.dto.SaveHomeContentRequest;
import com.diabetes.assistant.modules.content.entity.Article;
import com.diabetes.assistant.modules.content.entity.HomeContent;
import com.diabetes.assistant.modules.content.mapper.ArticleMapper;
import com.diabetes.assistant.modules.content.mapper.HomeContentMapper;
import com.diabetes.assistant.modules.content.service.ContentService;
import com.diabetes.assistant.modules.content.vo.AdminContentManagementResponse;
import com.diabetes.assistant.modules.content.vo.ArticleResponse;
import com.diabetes.assistant.modules.content.vo.HomeContentResponse;
import com.diabetes.assistant.modules.content.vo.HomeResponse;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService, ContentQueryApi {

    private static final String ARTICLE_DRAFT = "draft";
    private static final String ARTICLE_PUBLISHED = "published";
    private static final String ARTICLE_OFFLINE = "offline";
    private static final String HOME_ENABLED = "enabled";
    private static final String HOME_DISABLED = "disabled";
    private static final String TYPE_BANNER = "banner";
    private static final String TYPE_AI_DOCTOR_CARD = "ai_doctor_card";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final Set<String> ARTICLE_STATUSES = Set.of(ARTICLE_DRAFT, ARTICLE_PUBLISHED, ARTICLE_OFFLINE);
    private static final Set<String> ARTICLE_CATEGORIES = Set.of("diet", "exercise", "habit", "science", "complication", "mistake");
    private static final Set<String> HOME_TYPES = Set.of(TYPE_BANNER, TYPE_AI_DOCTOR_CARD);
    private static final Set<String> HOME_LINK_TYPES = Set.of("none", "article", "chat", "life_plan");
    private static final Set<String> HOME_STATUSES = Set.of(HOME_ENABLED, HOME_DISABLED);

    private final ArticleMapper articleMapper;
    private final HomeContentMapper homeContentMapper;
    private final UserQueryApi userQueryApi;

    @Override
    public String entry() {
        return "健康资讯与首页内容服务已启用";
    }

    @Override
    public HomeResponse getHome() {
        List<HomeContentResponse> contents = listEnabledHomeContentResponses();
        return HomeResponse.builder()
                .banners(contents.stream().filter(item -> TYPE_BANNER.equals(item.getContentType())).toList())
                .aiDoctorCards(contents.stream().filter(item -> TYPE_AI_DOCTOR_CARD.equals(item.getContentType())).toList())
                .recommendedArticles(listRecommendedArticleResponses())
                .build();
    }

    @Override
    public PageResult<ArticleResponse> listPublishedArticles(Integer page, Integer pageSize, String category, String keyword) {
        validateOptionalCategory(category);
        int currentPage = validPage(page);
        int currentPageSize = validPageSize(pageSize);
        String normalizedKeyword = normalize(keyword);

        Page<Article> result = articleMapper.selectPage(Page.of(currentPage, currentPageSize), new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, ARTICLE_PUBLISHED)
                .eq(StringUtils.hasText(category), Article::getCategory, normalize(category))
                .and(StringUtils.hasText(normalizedKeyword), nested -> nested
                        .like(Article::getTitle, normalizedKeyword)
                        .or()
                        .like(Article::getSummary, normalizedKeyword))
                .orderByAsc(Article::getSortOrder)
                .orderByDesc(Article::getUpdateTime)
                .orderByDesc(Article::getArticleId));

        return new PageResult<>(result.getRecords().stream()
                .map(article -> toArticleResponse(article, false, true))
                .toList(), result.getTotal(), currentPage, currentPageSize);
    }

    @Override
    @Transactional
    public ArticleResponse getPublishedArticleDetail(Integer articleId) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getArticleId, articleId)
                .eq(Article::getStatus, ARTICLE_PUBLISHED)
                .last("LIMIT 1"));
        if (article == null) {
            throw new BusinessException(404, "文章不存在或未上架");
        }

        int nextViewCount = article.getViewCount() == null ? 1 : article.getViewCount() + 1;
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getArticleId, articleId)
                .set(Article::getViewCount, nextViewCount)
                .set(Article::getUpdateTime, LocalDateTime.now()));
        article.setViewCount(nextViewCount);
        return toArticleResponse(article, true, true);
    }

    @Override
    public AdminContentManagementResponse getAdminContentManagement(Integer adminUserId, Integer page, Integer pageSize,
                                                                    String keyword, String articleStatus, String category,
                                                                    String homeContentType, String homeStatus) {
        requireAdmin(adminUserId);
        validateOptionalArticleStatus(articleStatus);
        validateOptionalCategory(category);
        validateOptionalHomeType(homeContentType);
        validateOptionalHomeStatus(homeStatus);

        int currentPage = validPage(page);
        int currentPageSize = validPageSize(pageSize);
        String normalizedKeyword = normalize(keyword);

        Page<Article> articlePage = articleMapper.selectPage(Page.of(currentPage, currentPageSize), new LambdaQueryWrapper<Article>()
                .eq(StringUtils.hasText(articleStatus), Article::getStatus, normalize(articleStatus))
                .eq(StringUtils.hasText(category), Article::getCategory, normalize(category))
                .and(StringUtils.hasText(normalizedKeyword), nested -> nested
                        .like(Article::getTitle, normalizedKeyword)
                        .or()
                        .like(Article::getSummary, normalizedKeyword))
                .orderByAsc(Article::getSortOrder)
                .orderByDesc(Article::getUpdateTime)
                .orderByDesc(Article::getArticleId));

        List<HomeContentResponse> homeContents = homeContentMapper.selectList(new LambdaQueryWrapper<HomeContent>()
                        .eq(StringUtils.hasText(homeContentType), HomeContent::getContentType, normalize(homeContentType))
                        .eq(StringUtils.hasText(homeStatus), HomeContent::getStatus, normalize(homeStatus))
                        .orderByAsc(HomeContent::getSortOrder)
                        .orderByDesc(HomeContent::getUpdateTime)
                        .orderByDesc(HomeContent::getContentId))
                .stream()
                .map(this::toHomeContentResponse)
                .toList();

        return AdminContentManagementResponse.builder()
                .articles(new PageResult<>(articlePage.getRecords().stream()
                        .map(article -> toArticleResponse(article, true, true))
                        .toList(), articlePage.getTotal(), currentPage, currentPageSize))
                .homeContents(homeContents)
                .build();
    }

    @Override
    @Transactional
    public ArticleResponse saveArticle(Integer adminUserId, SaveArticleRequest request) {
        requireAdmin(adminUserId);
        SaveArticleRequest safeRequest = request == null ? new SaveArticleRequest() : request;
        String title = required(safeRequest.getTitle(), "文章标题不能为空");
        String category = required(safeRequest.getCategory(), "文章分类不能为空");
        String content = required(safeRequest.getContent(), "文章正文不能为空");
        String status = StringUtils.hasText(safeRequest.getStatus()) ? normalize(safeRequest.getStatus()) : ARTICLE_DRAFT;
        validateArticleCategory(category);
        validateArticleStatus(status);

        LocalDateTime now = LocalDateTime.now();
        Article article = safeRequest.getArticleId() == null ? new Article() : articleMapper.selectById(safeRequest.getArticleId());
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }
        boolean creating = article.getArticleId() == null;
        article.setTitle(title);
        article.setCategory(category);
        article.setCoverImage(normalize(safeRequest.getCoverImage()));
        article.setSummary(normalize(safeRequest.getSummary()));
        article.setContent(content);
        article.setStatus(status);
        article.setIsRecommended(Boolean.TRUE.equals(toBoolean(safeRequest.getIsRecommended())) ? 1 : 0);
        article.setSortOrder(safeRequest.getSortOrder() == null ? 0 : safeRequest.getSortOrder());
        article.setUpdateTime(now);
        if (creating) {
            article.setViewCount(0);
            article.setCreateTime(now);
            articleMapper.insert(article);
        } else {
            articleMapper.updateById(article);
        }
        return toArticleResponse(article, true, true);
    }

    @Override
    @Transactional
    public HomeContentResponse saveHomeContent(Integer adminUserId, SaveHomeContentRequest request) {
        requireAdmin(adminUserId);
        SaveHomeContentRequest safeRequest = request == null ? new SaveHomeContentRequest() : request;
        String contentType = required(safeRequest.getContentType(), "首页内容类型不能为空");
        String title = required(safeRequest.getTitle(), "首页内容标题不能为空");
        String linkType = StringUtils.hasText(safeRequest.getLinkType()) ? normalize(safeRequest.getLinkType()) : "none";
        String status = StringUtils.hasText(safeRequest.getStatus()) ? normalize(safeRequest.getStatus()) : HOME_ENABLED;
        validateHomeType(contentType);
        validateHomeLinkType(linkType);
        validateHomeStatus(status);

        LocalDateTime now = LocalDateTime.now();
        HomeContent content = safeRequest.getContentId() == null ? new HomeContent() : homeContentMapper.selectById(safeRequest.getContentId());
        if (content == null) {
            throw new BusinessException(404, "首页内容不存在");
        }
        boolean creating = content.getContentId() == null;
        content.setContentType(contentType);
        content.setTitle(title);
        content.setSubtitle(normalize(safeRequest.getSubtitle()));
        content.setImageUrl(normalize(safeRequest.getImageUrl()));
        content.setLinkType(linkType);
        content.setLinkValue(normalize(safeRequest.getLinkValue()));
        content.setSortOrder(safeRequest.getSortOrder() == null ? 0 : safeRequest.getSortOrder());
        content.setStatus(status);
        content.setCreatedBy(adminUserId);
        content.setUpdateTime(now);
        if (creating) {
            content.setCreateTime(now);
            homeContentMapper.insert(content);
        } else {
            homeContentMapper.updateById(content);
        }
        return toHomeContentResponse(content);
    }

    @Override
    @Transactional
    public void deleteArticle(Integer adminUserId, Integer articleId) {
        requireAdmin(adminUserId);
        if (articleId == null) {
            throw new BusinessException(400, "文章 ID 不能为空");
        }
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getArticleId, articleId)
                .set(Article::getStatus, ARTICLE_OFFLINE)
                .set(Article::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    @Transactional
    public void deleteHomeContent(Integer adminUserId, Integer contentId) {
        requireAdmin(adminUserId);
        if (contentId == null) {
            throw new BusinessException(400, "内容 ID 不能为空");
        }
        int affected = homeContentMapper.deleteById(contentId);
        if (affected == 0) {
            throw new BusinessException(404, "首页内容不存在");
        }
    }

    @Override
    public ArticleSummaryDTO getArticleSummaryById(Integer articleId) {
        Article article = articleMapper.selectById(articleId);
        return article == null ? null : toArticleSummaryDTO(article);
    }

    @Override
    public List<ArticleSummaryDTO> listRecommendedArticles(Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 5 : Math.min(limit, 20);
        return articleMapper.selectList(new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, ARTICLE_PUBLISHED)
                        .eq(Article::getIsRecommended, 1)
                        .orderByAsc(Article::getSortOrder)
                        .orderByDesc(Article::getUpdateTime)
                        .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toArticleSummaryDTO)
                .toList();
    }

    @Override
    public List<HomeContentDTO> listEnabledHomeContents() {
        return homeContentMapper.selectList(new LambdaQueryWrapper<HomeContent>()
                        .eq(HomeContent::getStatus, HOME_ENABLED)
                        .orderByAsc(HomeContent::getSortOrder)
                        .orderByDesc(HomeContent::getUpdateTime))
                .stream()
                .map(this::toHomeContentDTO)
                .toList();
    }

    private List<HomeContentResponse> listEnabledHomeContentResponses() {
        return homeContentMapper.selectList(new LambdaQueryWrapper<HomeContent>()
                        .eq(HomeContent::getStatus, HOME_ENABLED)
                        .orderByAsc(HomeContent::getSortOrder)
                        .orderByDesc(HomeContent::getUpdateTime)
                        .orderByDesc(HomeContent::getContentId))
                .stream()
                .map(this::toHomeContentResponse)
                .toList();
    }

    private List<ArticleResponse> listRecommendedArticleResponses() {
        return articleMapper.selectList(new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, ARTICLE_PUBLISHED)
                        .eq(Article::getIsRecommended, 1)
                        .orderByAsc(Article::getSortOrder)
                        .orderByDesc(Article::getUpdateTime)
                        .orderByDesc(Article::getArticleId)
                        .last("LIMIT 10"))
                .stream()
                .map(article -> toArticleResponse(article, false, true))
                .toList();
    }

    private void requireAdmin(Integer userId) {
        if (!userQueryApi.isAdmin(userId)) {
            throw new BusinessException(403, "当前账号无管理员权限");
        }
    }

    private ArticleResponse toArticleResponse(Article article, boolean includeContent, boolean includeStatus) {
        return ArticleResponse.builder()
                .articleId(article.getArticleId())
                .title(article.getTitle())
                .category(article.getCategory())
                .coverImage(article.getCoverImage())
                .summary(article.getSummary())
                .content(includeContent ? article.getContent() : null)
                .status(includeStatus ? article.getStatus() : null)
                .viewCount(article.getViewCount())
                .isRecommended(article.getIsRecommended())
                .sortOrder(article.getSortOrder())
                .createTime(DateTimeUtil.format(article.getCreateTime()))
                .updateTime(DateTimeUtil.format(article.getUpdateTime()))
                .build();
    }

    private HomeContentResponse toHomeContentResponse(HomeContent content) {
        return HomeContentResponse.builder()
                .contentId(content.getContentId())
                .contentType(content.getContentType())
                .title(content.getTitle())
                .subtitle(content.getSubtitle())
                .imageUrl(content.getImageUrl())
                .linkType(content.getLinkType())
                .linkValue(content.getLinkValue())
                .sortOrder(content.getSortOrder())
                .status(content.getStatus())
                .createdBy(content.getCreatedBy())
                .createTime(DateTimeUtil.format(content.getCreateTime()))
                .updateTime(DateTimeUtil.format(content.getUpdateTime()))
                .build();
    }

    private ArticleSummaryDTO toArticleSummaryDTO(Article article) {
        ArticleSummaryDTO dto = new ArticleSummaryDTO();
        dto.setArticleId(article.getArticleId());
        dto.setTitle(article.getTitle());
        dto.setCategory(article.getCategory());
        dto.setCoverImage(article.getCoverImage());
        dto.setSummary(article.getSummary());
        dto.setViewCount(article.getViewCount());
        dto.setIsRecommended(article.getIsRecommended() != null && article.getIsRecommended() == 1);
        dto.setCreateTime(article.getCreateTime());
        return dto;
    }

    private HomeContentDTO toHomeContentDTO(HomeContent content) {
        HomeContentDTO dto = new HomeContentDTO();
        dto.setContentId(content.getContentId());
        dto.setContentType(content.getContentType());
        dto.setTitle(content.getTitle());
        dto.setSubtitle(content.getSubtitle());
        dto.setImageUrl(content.getImageUrl());
        dto.setLinkType(content.getLinkType());
        dto.setLinkValue(content.getLinkValue());
        dto.setSortOrder(content.getSortOrder());
        dto.setStatus(content.getStatus());
        return dto;
    }

    private void validateOptionalArticleStatus(String status) {
        String normalized = normalize(status);
        if (StringUtils.hasText(normalized)) {
            validateArticleStatus(normalized);
        }
    }

    private void validateArticleStatus(String status) {
        if (!ARTICLE_STATUSES.contains(status)) {
            throw new BusinessException(400, "文章状态不合法");
        }
    }

    private void validateOptionalCategory(String category) {
        String normalized = normalize(category);
        if (StringUtils.hasText(normalized)) {
            validateArticleCategory(normalized);
        }
    }

    private void validateArticleCategory(String category) {
        if (!ARTICLE_CATEGORIES.contains(category)) {
            throw new BusinessException(400, "文章分类不合法");
        }
    }

    private void validateOptionalHomeType(String contentType) {
        String normalized = normalize(contentType);
        if (StringUtils.hasText(normalized)) {
            validateHomeType(normalized);
        }
    }

    private void validateHomeType(String contentType) {
        if (!HOME_TYPES.contains(contentType)) {
            throw new BusinessException(400, "首页内容类型不合法");
        }
    }

    private void validateHomeLinkType(String linkType) {
        if (!HOME_LINK_TYPES.contains(linkType)) {
            throw new BusinessException(400, "首页内容跳转类型不合法");
        }
    }

    private void validateOptionalHomeStatus(String status) {
        String normalized = normalize(status);
        if (StringUtils.hasText(normalized)) {
            validateHomeStatus(normalized);
        }
    }

    private void validateHomeStatus(String status) {
        if (!HOME_STATUSES.contains(status)) {
            throw new BusinessException(400, "首页内容状态不合法");
        }
    }

    private String required(String value, String message) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(400, message);
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int validPage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int validPageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, 100);
    }

    private Boolean toBoolean(Integer value) {
        return value != null && value != 0;
    }
}
