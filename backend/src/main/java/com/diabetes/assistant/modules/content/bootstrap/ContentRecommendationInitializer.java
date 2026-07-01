package com.diabetes.assistant.modules.content.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.assistant.modules.content.entity.Article;
import com.diabetes.assistant.modules.content.entity.ArticleTag;
import com.diabetes.assistant.modules.content.mapper.ArticleMapper;
import com.diabetes.assistant.modules.content.mapper.ArticleTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class ContentRecommendationInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureTables();
        seedArticleTags();
    }

    private void ensureTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS article_tags (
                    tag_id INT NOT NULL AUTO_INCREMENT,
                    article_id INT NOT NULL,
                    tag_code VARCHAR(64) NOT NULL,
                    tag_name VARCHAR(64) NOT NULL,
                    tag_type VARCHAR(32) NOT NULL DEFAULT 'knowledge',
                    weight INT NOT NULL DEFAULT 10,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (tag_id),
                    KEY idx_article_tags_article (article_id),
                    KEY idx_article_tags_code (tag_code),
                    KEY idx_article_tags_type (tag_type),
                    CONSTRAINT fk_article_tags_article
                        FOREIGN KEY (article_id) REFERENCES articles(article_id)
                        ON DELETE CASCADE ON UPDATE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章知识标签'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS article_recommendation_logs (
                    recommendation_id INT NOT NULL AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    article_id INT NOT NULL,
                    scenario VARCHAR(32) NOT NULL,
                    rank_no INT NOT NULL,
                    score INT NOT NULL,
                    source_signals TEXT DEFAULT NULL,
                    reason_text TEXT DEFAULT NULL,
                    engine_type VARCHAR(64) NOT NULL DEFAULT 'patient_profile_plan_review_rule',
                    batch_key VARCHAR(80) DEFAULT NULL,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (recommendation_id),
                    KEY idx_article_reco_user_time (user_id, create_time),
                    KEY idx_article_reco_article (article_id),
                    KEY idx_article_reco_scenario (scenario),
                    KEY idx_article_reco_batch (batch_key),
                    CONSTRAINT fk_article_reco_user
                        FOREIGN KEY (user_id) REFERENCES users(user_id)
                        ON DELETE CASCADE ON UPDATE CASCADE,
                    CONSTRAINT fk_article_reco_article
                        FOREIGN KEY (article_id) REFERENCES articles(article_id)
                        ON DELETE CASCADE ON UPDATE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个性化科普推荐记录'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS article_read_events (
                    event_id INT NOT NULL AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    article_id INT NOT NULL,
                    recommendation_id INT DEFAULT NULL,
                    source_scenario VARCHAR(32) NOT NULL DEFAULT 'article_detail',
                    read_seconds INT NOT NULL DEFAULT 0,
                    progress_percent INT NOT NULL DEFAULT 0,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (event_id),
                    KEY idx_article_read_user_time (user_id, create_time),
                    KEY idx_article_read_article (article_id),
                    KEY idx_article_read_reco (recommendation_id),
                    CONSTRAINT fk_article_read_user
                        FOREIGN KEY (user_id) REFERENCES users(user_id)
                        ON DELETE CASCADE ON UPDATE CASCADE,
                    CONSTRAINT fk_article_read_article
                        FOREIGN KEY (article_id) REFERENCES articles(article_id)
                        ON DELETE CASCADE ON UPDATE CASCADE,
                    CONSTRAINT fk_article_read_reco
                        FOREIGN KEY (recommendation_id) REFERENCES article_recommendation_logs(recommendation_id)
                        ON DELETE SET NULL ON UPDATE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章阅读行为事件'
                """);
    }

    private void seedArticleTags() {
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, "published"));
        for (Article article : articles) {
            Long count = articleTagMapper.selectCount(new LambdaQueryWrapper<ArticleTag>()
                    .eq(ArticleTag::getArticleId, article.getArticleId()));
            if (count != null && count > 0) {
                continue;
            }
            for (TagSeed seed : inferTags(article)) {
                ArticleTag tag = new ArticleTag();
                tag.setArticleId(article.getArticleId());
                tag.setTagCode(seed.code());
                tag.setTagName(seed.name());
                tag.setTagType(seed.type());
                tag.setWeight(seed.weight());
                tag.setCreateTime(LocalDateTime.now());
                articleTagMapper.insert(tag);
            }
        }
    }

    private List<TagSeed> inferTags(Article article) {
        List<TagSeed> tags = new ArrayList<>();
        String category = article.getCategory() == null ? "" : article.getCategory();
        String text = String.join("\n",
                article.getTitle() == null ? "" : article.getTitle(),
                article.getSummary() == null ? "" : article.getSummary(),
                article.getContent() == null ? "" : article.getContent());
        if ("diet".equals(category) || containsAny(text, "餐盘", "主食", "饮食", "外食", "水果", "加餐")) {
            tags.add(tag("diet_control", "饮食结构优化", "intervention", 26));
        }
        if (containsAny(text, "餐后", "散步", "步行")) {
            tags.add(tag("post_meal_walk", "餐后步行", "plan_task", 30));
        }
        if ("exercise".equals(category) || containsAny(text, "运动", "有氧", "抗阻", "久坐", "鞋袜")) {
            tags.add(tag("exercise_plan", "运动计划", "intervention", 24));
        }
        if ("habit".equals(category) || containsAny(text, "作息", "睡眠", "压力", "饮水", "记录")) {
            tags.add(tag("sleep_stress", "作息与压力管理", "behavior", 20));
        }
        if ("science".equals(category) || containsAny(text, "血糖", "空腹", "餐后", "糖化", "指标")) {
            tags.add(tag("glucose_monitoring", "血糖监测", "metric", 28));
        }
        if ("complication".equals(category) || containsAny(text, "血压", "血脂", "眼", "足", "肾", "心血管", "并发症")) {
            tags.add(tag("complication", "并发症预防", "risk", 24));
        }
        if ("mistake".equals(category) || containsAny(text, "误区", "无糖", "保健品", "停药")) {
            tags.add(tag("mistake", "控糖误区", "education", 20));
        }
        if (containsAny(text, "BMI", "腰围", "体重", "肥胖")) {
            tags.add(tag("weight_management", "体重和腰围管理", "profile", 22));
        }
        if (containsAny(text, "高龄", "慢病", "安全", "低血糖", "疼痛")) {
            tags.add(tag("exercise_safety", "运动安全", "safety", 24));
        }
        if (tags.isEmpty()) {
            tags.add(tag("basic_science", "基础糖尿病科普", "knowledge", 10));
        }
        return tags;
    }

    private TagSeed tag(String code, String name, String type, int weight) {
        return new TagSeed(code, name, type, weight);
    }

    private boolean containsAny(String value, String... keywords) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private record TagSeed(String code, String name, String type, int weight) {
    }
}
