package com.diabetes.assistant.modules.content;

import com.diabetes.assistant.common.constants.RoleConstants;
import com.diabetes.assistant.common.constants.StatusConstants;
import com.diabetes.assistant.common.utils.JwtUtil;
import com.diabetes.assistant.common.utils.PasswordUtil;
import com.diabetes.assistant.modules.content.entity.Article;
import com.diabetes.assistant.modules.content.entity.HomeContent;
import com.diabetes.assistant.modules.content.mapper.ArticleMapper;
import com.diabetes.assistant.modules.content.mapper.HomeContentMapper;
import com.diabetes.assistant.modules.user.entity.User;
import com.diabetes.assistant.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private HomeContentMapper homeContentMapper;

    private Integer patientId;
    private Integer adminId;
    private Integer dietArticleId;
    private Integer draftArticleId;

    @BeforeEach
    void setUp() {
        homeContentMapper.delete(null);
        articleMapper.delete(null);
        userMapper.delete(null);

        patientId = insertUser("content_patient", "13870000001", RoleConstants.PATIENT);
        adminId = insertUser("content_admin", "13870000002", RoleConstants.ADMIN);

        dietArticleId = insertArticle("Diet published", "diet", "published", 1, 5);
        insertArticle("Exercise keyword", "exercise", "published", 0, 3);
        draftArticleId = insertArticle("Draft hidden", "diet", "draft", 0, 1);
        insertHomeContent("banner", "Home banner", "enabled", 1);
        insertHomeContent("ai_doctor_card", "AI doctor", "enabled", 2);
        insertHomeContent("banner", "Disabled banner", "disabled", 3);
    }

    @Test
    void homeReturnsEnabledContentsAndRecommendedArticles() throws Exception {
        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.banners.length()").value(1))
                .andExpect(jsonPath("$.data.ai_doctor_cards.length()").value(1))
                .andExpect(jsonPath("$.data.recommended_articles.length()").value(1))
                .andExpect(jsonPath("$.data.recommended_articles[0].content").doesNotExist());
    }

    @Test
    void articlesOnlyReturnPublishedAndSupportCategoryKeywordPaging() throws Exception {
        mockMvc.perform(get("/api/articles")
                        .param("page", "1")
                        .param("page_size", "1")
                        .param("category", "exercise")
                        .param("keyword", "keyword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].category").value("exercise"))
                .andExpect(jsonPath("$.data.list[0].content").doesNotExist());
    }

    @Test
    void articleDetailReturnsContentAndIncrementsViewCount() throws Exception {
        Article article = articleMapper.selectById(dietArticleId);
        Integer before = article.getViewCount();

        mockMvc.perform(get("/api/articles/{articleId}", article.getArticleId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Diet published content"))
                .andExpect(jsonPath("$.data.view_count").value(before + 1));

        assertThat(articleMapper.selectById(article.getArticleId()).getViewCount()).isEqualTo(before + 1);
    }

    @Test
    void offlineOrDraftArticleDetailReturns404() throws Exception {
        Article draft = articleMapper.selectById(draftArticleId);

        mockMvc.perform(get("/api/articles/{articleId}", draft.getArticleId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void patientCannotAccessAdminContentManagement() throws Exception {
        mockMvc.perform(get("/api/admin/content-management")
                        .header("Authorization", bearer(patientId, "content_patient", RoleConstants.PATIENT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void adminCanAccessContentManagement() throws Exception {
        mockMvc.perform(get("/api/admin/content-management")
                        .param("article_status", "published")
                        .param("home_status", "enabled")
                        .header("Authorization", bearer(adminId, "content_admin", RoleConstants.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.articles.total").value(2))
                .andExpect(jsonPath("$.data.home_contents.length()").value(2));
    }

    @Test
    void adminCanCreateAndUpdateArticle() throws Exception {
        String token = bearer(adminId, "content_admin", RoleConstants.ADMIN);

        String createResponse = mockMvc.perform(post("/api/admin/articles/save")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"New science","category":"science","summary":"science summary","content":"science body","status":"draft","is_recommended":0,"sort_order":9}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.article_id").exists())
                .andExpect(jsonPath("$.data.view_count").value(0))
                .andReturn().getResponse().getContentAsString();

        Integer articleId = Integer.valueOf(createResponse.replaceAll(".*\"article_id\":(\\d+).*", "$1"));
        mockMvc.perform(post("/api/admin/articles/save")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"article_id":%d,"title":"Updated science","category":"science","summary":"updated","content":"updated body","status":"published","is_recommended":1,"sort_order":2}
                                """.formatted(articleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated science"))
                .andExpect(jsonPath("$.data.status").value("published"))
                .andExpect(jsonPath("$.data.is_recommended").value(1));
    }

    @Test
    void invalidArticleCategoryOrStatusReturns400() throws Exception {
        String token = bearer(adminId, "content_admin", RoleConstants.ADMIN);

        mockMvc.perform(post("/api/admin/articles/save")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Bad","category":"bad","content":"body","status":"published"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/admin/articles/save")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Bad","category":"diet","content":"body","status":"deleted"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void adminCanSaveHomeContent() throws Exception {
        mockMvc.perform(post("/api/admin/home-contents/save")
                        .header("Authorization", bearer(adminId, "content_admin", RoleConstants.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content_type":"banner","title":"New banner","subtitle":"sub","image_url":"/banner.png","link_type":"article","link_value":"1","sort_order":4,"status":"enabled"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content_id").exists())
                .andExpect(jsonPath("$.data.created_by").value(adminId))
                .andExpect(jsonPath("$.data.content_type").value("banner"));
    }

    @Test
    void invalidHomeContentFieldsReturn400() throws Exception {
        String token = bearer(adminId, "content_admin", RoleConstants.ADMIN);

        mockMvc.perform(post("/api/admin/home-contents/save")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content_type":"doctor","title":"Bad","link_type":"none","status":"enabled"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/home-contents/save")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content_type":"banner","title":"Bad","link_type":"external","status":"enabled"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/home-contents/save")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content_type":"banner","title":"Bad","link_type":"none","status":"deleted"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private Integer insertUser(String username, String phone, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setEmail(username + "@example.com");
        user.setAvatar("/uploads/avatar/default.png");
        user.setPasswordHash(PasswordUtil.hashPassword("123456"));
        user.setRole(role);
        user.setStatus(StatusConstants.ACTIVE);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user.getUserId();
    }

    private Integer insertArticle(String title, String category, String status, int recommended, int sortOrder) {
        Article article = new Article();
        article.setTitle(title);
        article.setCategory(category);
        article.setCoverImage("/cover.png");
        article.setSummary(title + " summary");
        article.setContent(title + " content");
        article.setStatus(status);
        article.setViewCount(10);
        article.setIsRecommended(recommended);
        article.setSortOrder(sortOrder);
        article.setCreateTime(LocalDateTime.now().minusDays(sortOrder));
        article.setUpdateTime(LocalDateTime.now().minusDays(sortOrder));
        articleMapper.insert(article);
        return article.getArticleId();
    }

    private void insertHomeContent(String type, String title, String status, int sortOrder) {
        HomeContent content = new HomeContent();
        content.setContentType(type);
        content.setTitle(title);
        content.setSubtitle(title + " subtitle");
        content.setImageUrl("/home.png");
        content.setLinkType(TYPE_AI_DOCTOR_CARD.equals(type) ? "chat" : "article");
        content.setLinkValue(TYPE_AI_DOCTOR_CARD.equals(type) ? "chat" : "1");
        content.setSortOrder(sortOrder);
        content.setStatus(status);
        content.setCreatedBy(adminId);
        content.setCreateTime(LocalDateTime.now().minusDays(sortOrder));
        content.setUpdateTime(LocalDateTime.now().minusDays(sortOrder));
        homeContentMapper.insert(content);
    }

    private String bearer(Integer userId, String username, String role) {
        return "Bearer " + jwtUtil.generateToken(userId, username, role);
    }

    private static final String TYPE_AI_DOCTOR_CARD = "ai_doctor_card";
}
