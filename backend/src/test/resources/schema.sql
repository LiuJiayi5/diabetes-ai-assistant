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
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone)
);

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
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (profile_id)
);

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
    PRIMARY KEY (metric_id)
);

CREATE TABLE risk_assessments (
    assessment_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    metric_id INT DEFAULT NULL,
    risk_level VARCHAR(20) NOT NULL,
    risk_score INT DEFAULT NULL,
    diabetes_type_tendency VARCHAR(50) DEFAULT NULL,
    main_risk_factors TEXT DEFAULT NULL,
    indicator_analysis TEXT DEFAULT NULL,
    health_advice TEXT DEFAULT NULL,
    medical_warning TEXT DEFAULT NULL,
    summary TEXT DEFAULT NULL,
    call_status VARCHAR(20) NOT NULL DEFAULT 'success',
    error_message TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (assessment_id)
);

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
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    call_status VARCHAR(20) NOT NULL DEFAULT 'success',
    error_message TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (plan_id)
);

CREATE TABLE articles (
    article_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    category VARCHAR(32) NOT NULL,
    cover_image VARCHAR(255) DEFAULT NULL,
    summary VARCHAR(255) DEFAULT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    view_count INT NOT NULL DEFAULT 0,
    is_recommended TINYINT NOT NULL DEFAULT 0,
    sort_order INT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (article_id)
);

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
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (content_id)
);
