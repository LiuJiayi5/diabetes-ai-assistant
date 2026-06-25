CREATE DATABASE IF NOT EXISTS diabetes_assistant DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE diabetes_assistant;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS checkin_analysis;
DROP TABLE IF EXISTS checkin_records;
DROP TABLE IF EXISTS ai_chat_messages;
DROP TABLE IF EXISTS ai_chat_sessions;
DROP TABLE IF EXISTS home_contents;
DROP TABLE IF EXISTS articles;
DROP TABLE IF EXISTS life_plans;
DROP TABLE IF EXISTS risk_assessments;
DROP TABLE IF EXISTS health_metrics;
DROP TABLE IF EXISTS patient_profiles;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    user_id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    avatar VARCHAR(255) DEFAULT NULL,
    role ENUM('patient','admin') NOT NULL DEFAULT 'patient',
    status ENUM('active','disabled') NOT NULL DEFAULT 'active',
    last_login_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    KEY idx_role (role),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE patient_profiles (
    profile_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    age INT NOT NULL,
    gender ENUM('male','female') NOT NULL,
    height_cm DECIMAL(5,1) NOT NULL,
    base_weight_kg DECIMAL(5,1) NOT NULL,
    base_waist_cm DECIMAL(5,1) DEFAULT NULL,
    family_history TEXT DEFAULT NULL,
    chronic_history TEXT DEFAULT NULL,
    allergy_history TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (profile_id),
    UNIQUE KEY uk_patient_profiles_user_id (user_id),
    CONSTRAINT fk_patient_profiles_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    recorded_at DATE NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (metric_id),
    KEY idx_health_metrics_user_id (user_id),
    KEY idx_health_metrics_recorded_at (recorded_at),
    CONSTRAINT fk_health_metrics_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE risk_assessments (
    assessment_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    metric_id INT DEFAULT NULL,
    risk_level ENUM('low','medium','high') NOT NULL,
    risk_score INT DEFAULT NULL,
    diabetes_type_tendency VARCHAR(50) DEFAULT NULL,
    main_risk_factors TEXT DEFAULT NULL,
    indicator_analysis TEXT DEFAULT NULL,
    health_advice TEXT DEFAULT NULL,
    medical_warning TEXT DEFAULT NULL,
    summary TEXT DEFAULT NULL,
    call_status ENUM('success','failed') NOT NULL DEFAULT 'success',
    error_message TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (assessment_id),
    KEY idx_risk_assessments_user_id (user_id),
    KEY idx_risk_assessments_metric_id (metric_id),
    KEY idx_risk_assessments_call_status (call_status),
    KEY idx_risk_assessments_create_time (create_time),
    CONSTRAINT fk_risk_assessments_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_risk_assessments_metric FOREIGN KEY (metric_id) REFERENCES health_metrics(metric_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE life_plans (
    plan_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    profile_id INT NOT NULL,
    metric_id INT NOT NULL,
    assessment_id INT NOT NULL,
    plan_title VARCHAR(100) NOT NULL,
    plan_goal VARCHAR(100) DEFAULT NULL,
    input_summary TEXT DEFAULT NULL,
    plan_json TEXT NOT NULL,
    checkin_tasks_json TEXT DEFAULT NULL,
    summary TEXT DEFAULT NULL,
    status ENUM('active','history') NOT NULL DEFAULT 'active',
    call_status ENUM('success','failed') NOT NULL DEFAULT 'success',
    error_message TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (plan_id),
    KEY idx_life_plans_user_id (user_id),
    KEY idx_life_plans_profile_id (profile_id),
    KEY idx_life_plans_metric_id (metric_id),
    KEY idx_life_plans_assessment_id (assessment_id),
    KEY idx_life_plans_status (status),
    KEY idx_life_plans_call_status (call_status),
    KEY idx_life_plans_create_time (create_time),
    CONSTRAINT fk_life_plans_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_life_plans_profile FOREIGN KEY (profile_id) REFERENCES patient_profiles(profile_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_life_plans_metric FOREIGN KEY (metric_id) REFERENCES health_metrics(metric_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_life_plans_assessment FOREIGN KEY (assessment_id) REFERENCES risk_assessments(assessment_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE articles (
    article_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    category ENUM('diet','exercise','habit','science','complication','mistake') NOT NULL,
    cover_image VARCHAR(255) DEFAULT NULL,
    summary VARCHAR(255) DEFAULT NULL,
    content TEXT NOT NULL,
    status ENUM('draft','published','offline') NOT NULL DEFAULT 'draft',
    view_count INT NOT NULL DEFAULT 0,
    is_recommended TINYINT(1) NOT NULL DEFAULT 0,
    sort_order INT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (article_id),
    KEY idx_articles_status (status),
    KEY idx_articles_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE home_contents (
    content_id INT NOT NULL AUTO_INCREMENT,
    content_type ENUM('banner','ai_doctor_card') NOT NULL,
    title VARCHAR(100) NOT NULL,
    subtitle VARCHAR(255) DEFAULT NULL,
    image_url VARCHAR(255) DEFAULT NULL,
    link_type ENUM('none','article','chat','life_plan') NOT NULL DEFAULT 'none',
    link_value VARCHAR(100) DEFAULT NULL,
    sort_order INT DEFAULT NULL,
    status ENUM('enabled','disabled') NOT NULL DEFAULT 'enabled',
    created_by INT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (content_id),
    KEY idx_home_contents_created_by (created_by),
    CONSTRAINT fk_home_contents_created_by FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_chat_sessions (
    session_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    session_title VARCHAR(100) DEFAULT NULL,
    dify_conversation_id VARCHAR(128) DEFAULT NULL,
    status ENUM('active','deleted') NOT NULL DEFAULT 'active',
    last_message_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (session_id),
    KEY idx_ai_chat_sessions_user_id (user_id),
    CONSTRAINT fk_ai_chat_sessions_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_chat_messages (
    message_id INT NOT NULL AUTO_INCREMENT,
    session_id INT NOT NULL,
    user_id INT NOT NULL,
    user_message TEXT NOT NULL,
    ai_response TEXT DEFAULT NULL,
    context_summary TEXT DEFAULT NULL,
    call_status ENUM('success','failed') NOT NULL DEFAULT 'success',
    error_message TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id),
    KEY idx_ai_chat_messages_session_id (session_id),
    KEY idx_ai_chat_messages_user_id (user_id),
    CONSTRAINT fk_ai_chat_messages_session FOREIGN KEY (session_id) REFERENCES ai_chat_sessions(session_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ai_chat_messages_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE checkin_records (
    checkin_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    plan_id INT NOT NULL,
    task_type ENUM('diet','exercise') NOT NULL,
    task_name VARCHAR(100) NOT NULL,
    status ENUM('pending','completed','missed') NOT NULL DEFAULT 'pending',
    note TEXT DEFAULT NULL,
    checkin_date DATE NOT NULL,
    completed_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (checkin_id),
    KEY idx_checkin_records_user_id (user_id),
    KEY idx_checkin_records_plan_id (plan_id),
    KEY idx_checkin_records_checkin_date (checkin_date),
    CONSTRAINT fk_checkin_records_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_checkin_records_plan FOREIGN KEY (plan_id) REFERENCES life_plans(plan_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE checkin_analysis (
    analysis_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    plan_id INT NOT NULL,
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
    call_status ENUM('success','failed') NOT NULL DEFAULT 'success',
    error_message TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (analysis_id),
    KEY idx_checkin_analysis_user_id (user_id),
    KEY idx_checkin_analysis_plan_id (plan_id),
    CONSTRAINT fk_checkin_analysis_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_checkin_analysis_plan FOREIGN KEY (plan_id) REFERENCES life_plans(plan_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
