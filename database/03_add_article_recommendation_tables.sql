-- Incremental migration for personalized article recommendation.
-- Additive only: no DROP, TRUNCATE, DELETE, or seed data.
-- Safe to run on an existing database that already has users and articles.

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Article knowledge tags';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Personalized article recommendation logs';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Article read behavior events';
