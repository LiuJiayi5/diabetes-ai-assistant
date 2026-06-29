USE diabetes_assistant;

ALTER TABLE life_plans
    ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'manual' AFTER error_message,
    ADD COLUMN source_review_id INT DEFAULT NULL AFTER source_type,
    ADD KEY idx_life_plans_source_review (source_review_id);

CREATE TABLE IF NOT EXISTS intervention_reviews (
    review_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    plan_id INT DEFAULT NULL,
    source_review_id INT DEFAULT NULL,
    trigger_type VARCHAR(64) NOT NULL,
    trigger_reason VARCHAR(255) DEFAULT NULL,
    period_start_date DATE DEFAULT NULL,
    period_end_date DATE DEFAULT NULL,
    review_days INT DEFAULT NULL,
    plan_day INT DEFAULT NULL,
    adherence_score INT DEFAULT NULL,
    intervention_level VARCHAR(40) DEFAULT NULL,
    should_update_plan TINYINT(1) NOT NULL DEFAULT 0,
    update_scope VARCHAR(64) DEFAULT NULL,
    affected_days TEXT DEFAULT NULL,
    main_problem_tags TEXT DEFAULT NULL,
    preserved_items TEXT DEFAULT NULL,
    changed_items TEXT DEFAULT NULL,
    adjustment_strategy TEXT DEFAULT NULL,
    patient_notice TEXT DEFAULT NULL,
    explanation TEXT DEFAULT NULL,
    safety_warning TEXT DEFAULT NULL,
    adjusted_plan_patch TEXT DEFAULT NULL,
    generated_plan_id INT DEFAULT NULL,
    input_summary TEXT DEFAULT NULL,
    raw_response TEXT DEFAULT NULL,
    call_status VARCHAR(20) NOT NULL DEFAULT 'success',
    error_message TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id),
    KEY idx_intervention_reviews_user (user_id),
    KEY idx_intervention_reviews_plan (plan_id),
    KEY idx_intervention_reviews_trigger (trigger_type, create_time),
    KEY idx_intervention_reviews_level (intervention_level),
    KEY idx_intervention_reviews_generated_plan (generated_plan_id),
    CONSTRAINT fk_intervention_reviews_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_intervention_reviews_plan
        FOREIGN KEY (plan_id) REFERENCES life_plans(plan_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_intervention_reviews_generated_plan
        FOREIGN KEY (generated_plan_id) REFERENCES life_plans(plan_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='intervention review';
