CREATE DATABASE IF NOT EXISTS diabetes_assistant
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE diabetes_assistant;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS api_call_logs;
DROP TABLE IF EXISTS checkin_analysis;
DROP TABLE IF EXISTS checkin_records;
DROP TABLE IF EXISTS ai_chat_messages;
DROP TABLE IF EXISTS ai_chat_sessions;
DROP TABLE IF EXISTS ai_experts;
DROP TABLE IF EXISTS home_contents;
DROP TABLE IF EXISTS articles;
DROP TABLE IF EXISTS life_plans;
DROP TABLE IF EXISTS risk_assessments;
DROP TABLE IF EXISTS health_metrics;
DROP TABLE IF EXISTS patient_profiles;
DROP TABLE IF EXISTS email_verification_codes;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    user_id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    avatar VARCHAR(255) DEFAULT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'patient',
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    last_login_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_phone (phone),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role (role),
    KEY idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号';

CREATE TABLE email_verification_codes (
    code_id INT NOT NULL AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    used TINYINT(1) NOT NULL DEFAULT 0,
    expires_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_time DATETIME DEFAULT NULL,
    PRIMARY KEY (code_id),
    KEY idx_email_codes_lookup (email, purpose, used, expires_at),
    KEY idx_email_codes_created (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮箱验证码';

CREATE TABLE patient_profiles (
    profile_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(20) NOT NULL,
    height_cm DECIMAL(5,1) NOT NULL,
    base_weight_kg DECIMAL(5,1) NOT NULL,
    base_waist_cm DECIMAL(5,1) DEFAULT NULL,
    family_history TEXT DEFAULT NULL,
    chronic_history TEXT DEFAULT NULL,
    allergy_history TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (profile_id),
    UNIQUE KEY uk_patient_profiles_user (user_id),
    CONSTRAINT fk_patient_profiles_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者健康档案';

CREATE TABLE health_metrics (
    metric_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    weight_kg DECIMAL(5,1) DEFAULT NULL,
    waist_cm DECIMAL(5,1) DEFAULT NULL,
    systolic_bp INT DEFAULT NULL,
    diastolic_bp INT DEFAULT NULL,
    fasting_glucose DECIMAL(4,1) DEFAULT NULL,
    postprandial_glucose DECIMAL(4,1) DEFAULT NULL,
    hba1c DECIMAL(3,1) DEFAULT NULL,
    diet_status TEXT DEFAULT NULL,
    exercise_status TEXT DEFAULT NULL,
    recorded_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (metric_id),
    KEY idx_health_metrics_user (user_id),
    KEY idx_health_metrics_recorded_at (recorded_at),
    CONSTRAINT fk_health_metrics_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康数据记录';

CREATE TABLE risk_assessments (
    assessment_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    metric_id INT DEFAULT NULL,
    request_summary TEXT DEFAULT NULL,
    response_result TEXT DEFAULT NULL,
    risk_level VARCHAR(20) DEFAULT NULL,
    risk_score INT DEFAULT NULL,
    diabetes_type_tendency VARCHAR(80) DEFAULT NULL,
    main_risk_factors TEXT DEFAULT NULL,
    indicator_analysis TEXT DEFAULT NULL,
    health_advice TEXT DEFAULT NULL,
    medical_warning TEXT DEFAULT NULL,
    summary TEXT DEFAULT NULL,
    call_status VARCHAR(20) NOT NULL DEFAULT 'success',
    error_message TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (assessment_id),
    KEY idx_risk_assessments_user (user_id),
    KEY idx_risk_assessments_metric (metric_id),
    KEY idx_risk_assessments_level (risk_level),
    KEY idx_risk_assessments_call_status (call_status),
    KEY idx_risk_assessments_created (create_time),
    CONSTRAINT fk_risk_assessments_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_risk_assessments_metric
        FOREIGN KEY (metric_id) REFERENCES health_metrics(metric_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='糖尿病风险评估';

CREATE TABLE life_plans (
    plan_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    profile_id INT DEFAULT NULL,
    metric_id INT DEFAULT NULL,
    assessment_id INT DEFAULT NULL,
    plan_title VARCHAR(100) DEFAULT NULL,
    plan_goal VARCHAR(100) DEFAULT NULL,
    input_summary TEXT DEFAULT NULL,
    plan_json TEXT DEFAULT NULL,
    checkin_tasks_json TEXT DEFAULT NULL,
    summary TEXT DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    call_status VARCHAR(20) NOT NULL DEFAULT 'success',
    error_message TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (plan_id),
    KEY idx_life_plans_user (user_id),
    KEY idx_life_plans_profile (profile_id),
    KEY idx_life_plans_metric (metric_id),
    KEY idx_life_plans_assessment (assessment_id),
    KEY idx_life_plans_status (status),
    KEY idx_life_plans_call_status (call_status),
    KEY idx_life_plans_created (create_time),
    CONSTRAINT fk_life_plans_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_life_plans_profile
        FOREIGN KEY (profile_id) REFERENCES patient_profiles(profile_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_life_plans_metric
        FOREIGN KEY (metric_id) REFERENCES health_metrics(metric_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_life_plans_assessment
        FOREIGN KEY (assessment_id) REFERENCES risk_assessments(assessment_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个性化生活方案';

CREATE TABLE articles (
    article_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    category VARCHAR(32) NOT NULL,
    cover_image VARCHAR(255) DEFAULT NULL,
    summary VARCHAR(255) DEFAULT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    view_count INT NOT NULL DEFAULT 0,
    is_recommended TINYINT(1) NOT NULL DEFAULT 0,
    sort_order INT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (article_id),
    KEY idx_articles_status (status),
    KEY idx_articles_category (category),
    KEY idx_articles_recommended (is_recommended, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康资讯文章';

CREATE TABLE home_contents (
    content_id INT NOT NULL AUTO_INCREMENT,
    content_type VARCHAR(32) NOT NULL,
    title VARCHAR(100) NOT NULL,
    subtitle VARCHAR(255) DEFAULT NULL,
    image_url VARCHAR(255) DEFAULT NULL,
    link_type VARCHAR(32) NOT NULL DEFAULT 'none',
    link_value VARCHAR(100) DEFAULT NULL,
    sort_order INT DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'enabled',
    created_by INT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (content_id),
    KEY idx_home_contents_type_status (content_type, status, sort_order),
    KEY idx_home_contents_created_by (created_by),
    CONSTRAINT fk_home_contents_created_by
        FOREIGN KEY (created_by) REFERENCES users(user_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页展示内容';

CREATE TABLE ai_experts (
    expert_id INT NOT NULL AUTO_INCREMENT,
    expert_name VARCHAR(80) NOT NULL,
    title VARCHAR(120) DEFAULT NULL,
    department VARCHAR(120) DEFAULT NULL,
    avatar_url VARCHAR(255) DEFAULT NULL,
    specialty VARCHAR(255) DEFAULT NULL,
    persona TEXT DEFAULT NULL,
    opening_message TEXT DEFAULT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'enabled',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (expert_id),
    KEY idx_ai_experts_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 专家身份';

CREATE TABLE ai_chat_sessions (
    session_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    expert_id INT DEFAULT NULL,
    session_title VARCHAR(100) DEFAULT NULL,
    dify_conversation_id VARCHAR(128) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    last_message_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (session_id),
    KEY idx_ai_chat_sessions_user (user_id),
    KEY idx_ai_chat_sessions_expert (expert_id),
    KEY idx_ai_chat_sessions_status_time (status, last_message_time),
    CONSTRAINT fk_ai_chat_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ai_chat_sessions_expert
        FOREIGN KEY (expert_id) REFERENCES ai_experts(expert_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 咨询会话';

CREATE TABLE ai_chat_messages (
    message_id INT NOT NULL AUTO_INCREMENT,
    session_id INT NOT NULL,
    user_id INT NOT NULL,
    user_message TEXT NOT NULL,
    ai_response TEXT DEFAULT NULL,
    context_summary TEXT DEFAULT NULL,
    call_status VARCHAR(20) NOT NULL DEFAULT 'success',
    error_message TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id),
    KEY idx_ai_chat_messages_session (session_id),
    KEY idx_ai_chat_messages_user (user_id),
    KEY idx_ai_chat_messages_status_time (call_status, create_time),
    CONSTRAINT fk_ai_chat_messages_session
        FOREIGN KEY (session_id) REFERENCES ai_chat_sessions(session_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ai_chat_messages_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 咨询消息';

CREATE TABLE checkin_records (
    checkin_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    plan_id INT DEFAULT NULL,
    task_type VARCHAR(20) NOT NULL,
    task_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    note TEXT DEFAULT NULL,
    checkin_date DATE NOT NULL,
    completed_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (checkin_id),
    KEY idx_checkin_records_user (user_id),
    KEY idx_checkin_records_plan (plan_id),
    KEY idx_checkin_records_date (checkin_date),
    KEY idx_checkin_records_status (status),
    CONSTRAINT fk_checkin_records_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_checkin_records_plan
        FOREIGN KEY (plan_id) REFERENCES life_plans(plan_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生活打卡记录';

CREATE TABLE checkin_analysis (
    analysis_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    plan_id INT DEFAULT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days INT NOT NULL,
    diet_completion_count INT NOT NULL,
    exercise_completion_count INT NOT NULL,
    completion_rate DECIMAL(5,2) NOT NULL,
    habit_score INT DEFAULT NULL,
    diet_summary TEXT DEFAULT NULL,
    exercise_summary TEXT DEFAULT NULL,
    life_evaluation TEXT DEFAULT NULL,
    main_problems TEXT DEFAULT NULL,
    improvement_suggestions TEXT DEFAULT NULL,
    next_focus TEXT DEFAULT NULL,
    summary TEXT DEFAULT NULL,
    input_summary TEXT DEFAULT NULL,
    call_status VARCHAR(20) NOT NULL DEFAULT 'success',
    error_message TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (analysis_id),
    KEY idx_checkin_analysis_user (user_id),
    KEY idx_checkin_analysis_plan (plan_id),
    KEY idx_checkin_analysis_status_time (call_status, create_time),
    CONSTRAINT fk_checkin_analysis_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_checkin_analysis_plan
        FOREIGN KEY (plan_id) REFERENCES life_plans(plan_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡行为分析';

CREATE TABLE api_call_logs (
    log_id INT NOT NULL AUTO_INCREMENT,
    user_id INT DEFAULT NULL,
    service_type VARCHAR(64) NOT NULL,
    request_summary TEXT DEFAULT NULL,
    response_summary TEXT DEFAULT NULL,
    call_status VARCHAR(20) NOT NULL DEFAULT 'success',
    error_message TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_api_call_logs_user (user_id),
    KEY idx_api_call_logs_service (service_type),
    KEY idx_api_call_logs_status_time (call_status, create_time),
    CONSTRAINT fk_api_call_logs_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 接口调用日志';
