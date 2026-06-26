-- ============================================================
-- 糖尿病预治智能助手系统 - 数据库建表语句
-- 
-- 项目：糖尿病预治智能助手（Web前端 + 后端服务 + MySQL + Dify + DeepSeek）
-- 数据库：MySQL 8.0+
-- 引擎：InnoDB
-- 字符集：utf8mb4
-- 
-- 基于文档：
--   - 《系统功能整理与模块划分文档-v2.md》
--   - 《全局接口设计规范.md》
--   - 《模块1-6-7接口设计文档.md》
--   - 《模块5、模块8接口设计文档.md》
-- 
-- 命名规范：
--   - 表名：小写 + 下划线，复数形式
--   - 字段名：snake_case（与接口 JSON 字段名一致）
--   - 主键：xxx_id，自增
--   - 时间字段：create_time, update_time, delete_time, completed_time
--   - 枚举值：英文小写
-- 
-- 创建日期：2026-06-23
-- ============================================================

-- 创建数据库（如尚未创建）
CREATE DATABASE IF NOT EXISTS diabetes_assistant
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE diabetes_assistant;

-- ============================================================
-- 模块1：用户认证与基础信息模块
-- ============================================================

-- -----------------------------------------------------------
-- 表1：users（用户表）
-- 说明：存储患者用户和管理员的账号信息
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id             INT             NOT NULL AUTO_INCREMENT  COMMENT '用户唯一编号',
    username            VARCHAR(64)     NOT NULL                COMMENT '用户名，用于登录和展示',
    password_hash       VARCHAR(255)    NOT NULL                COMMENT '加密后的密码哈希，禁止返回给前端',
    phone               VARCHAR(20)     DEFAULT NULL            COMMENT '手机号',
    email               VARCHAR(128)    DEFAULT NULL            COMMENT '邮箱',
    avatar              VARCHAR(512)    DEFAULT NULL            COMMENT '头像地址',
    role                VARCHAR(16)     NOT NULL DEFAULT 'patient'
                        COMMENT '用户角色：patient（患者用户）/ admin（管理员）',
    status              VARCHAR(16)     NOT NULL DEFAULT 'active'
                        COMMENT '账号状态：active（正常）/ disabled（禁用）',
    last_login_time     DATETIME        DEFAULT NULL            COMMENT '最近登录时间',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone    (phone),
    INDEX idx_role   (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';


-- ============================================================
-- 模块2：健康档案管理模块
-- ============================================================

-- -----------------------------------------------------------
-- 表2：patient_profiles（患者健康档案表）
-- 说明：存储患者用户的长期稳定健康画像数据，
--       为风险预测、生活方案、AI 咨询和打卡分析提供基础数据
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS patient_profiles (
    profile_id          INT             NOT NULL AUTO_INCREMENT  COMMENT '档案编号',
    user_id             INT             NOT NULL                COMMENT '所属用户编号',
    age                 INT             DEFAULT NULL            COMMENT '年龄',
    gender              VARCHAR(8)      DEFAULT NULL
                        COMMENT '性别：male（男）/ female（女）/ other（其他）',
    height_cm           DECIMAL(5,1)    DEFAULT NULL            COMMENT '身高（厘米）',
    base_weight_kg      DECIMAL(5,1)    DEFAULT NULL            COMMENT '基础体重（公斤）',
    base_waist_cm       DECIMAL(5,1)    DEFAULT NULL            COMMENT '基础腰围（厘米）',
    family_history      TEXT            DEFAULT NULL            COMMENT '家族病史',
    chronic_history     TEXT            DEFAULT NULL            COMMENT '既往慢病史（如高血压、心脏病等）',
    allergy_history     TEXT            DEFAULT NULL            COMMENT '过敏史（可选）',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (profile_id),
    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_gender (gender),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者健康档案表';


-- ============================================================
-- 模块3：健康数据录入模块
-- ============================================================

-- -----------------------------------------------------------
-- 表3：health_metrics（健康数据指标表）
-- 说明：存储患者用户的动态健康指标记录，
--       每次录入生成一条新记录，反映近期健康状态
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS health_metrics (
    metric_id               INT             NOT NULL AUTO_INCREMENT  COMMENT '健康数据编号',
    user_id                 INT             NOT NULL                COMMENT '所属用户编号',
    weight_kg               DECIMAL(5,1)    DEFAULT NULL            COMMENT '当前体重（公斤）',
    waist_cm                DECIMAL(5,1)    DEFAULT NULL            COMMENT '腰围（厘米）',
    systolic_bp             INT             DEFAULT NULL            COMMENT '收缩压（mmHg）',
    diastolic_bp            INT             DEFAULT NULL            COMMENT '舒张压（mmHg）',
    fasting_glucose         DECIMAL(4,1)    DEFAULT NULL            COMMENT '空腹血糖（mmol/L）',
    postprandial_glucose    DECIMAL(4,1)    DEFAULT NULL            COMMENT '餐后血糖（mmol/L）',
    hba1c                   DECIMAL(3,1)    DEFAULT NULL            COMMENT '糖化血红蛋白（%），可选',
    diet_status             VARCHAR(64)     DEFAULT NULL            COMMENT '饮食基础状态',
    exercise_status         VARCHAR(64)     DEFAULT NULL            COMMENT '运动基础状态',
    recorded_at             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间（用户实际测量或录入的日期时间）',
    create_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (metric_id),
    INDEX idx_user_id       (user_id),
    INDEX idx_recorded_at   (recorded_at),
    INDEX idx_fasting_glucose (fasting_glucose),
    CONSTRAINT fk_metric_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康数据指标表';


-- ============================================================
-- 模块4：糖尿病风险预测模块
-- ============================================================

-- -----------------------------------------------------------
-- 表4：risk_assessments（风险评估记录表）
-- 说明：存储每次调用 Dify 糖尿病风险预测工作流的结果
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS risk_assessments (
    assessment_id       INT             NOT NULL AUTO_INCREMENT  COMMENT '评估记录编号',
    user_id             INT             NOT NULL                COMMENT '用户编号',
    request_summary     TEXT            DEFAULT NULL            COMMENT '输入摘要：本次传入 Dify 的健康档案和指标摘要',
    response_result     JSON            DEFAULT NULL            COMMENT '工作流返回的完整 JSON 结果',
    risk_level          VARCHAR(16)     DEFAULT NULL
                        COMMENT '风险等级：low（低风险）/ medium（中风险）/ high（高风险）',
    risk_score          INT             DEFAULT NULL            COMMENT '风险评分（可选）',
    call_status         VARCHAR(16)     NOT NULL DEFAULT 'success'
                        COMMENT 'Dify 调用状态：success（成功）/ failed（失败）',
    error_message       TEXT            DEFAULT NULL            COMMENT '调用失败原因，成功时为空',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
    PRIMARY KEY (assessment_id),
    INDEX idx_user_id       (user_id),
    INDEX idx_risk_level    (risk_level),
    INDEX idx_call_status   (call_status),
    INDEX idx_create_time   (create_time),
    CONSTRAINT fk_assessment_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='糖尿病风险评估记录表';


-- ============================================================
-- 模块5：AI 医生咨询模块
-- ============================================================

-- -----------------------------------------------------------
-- 表5：ai_chat_sessions（AI 咨询会话表）
-- 说明：存储 AI 医生咨询的会话信息
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_chat_sessions (
    session_id          INT             NOT NULL AUTO_INCREMENT  COMMENT '会话编号',
    user_id             INT             NOT NULL                COMMENT '用户编号',
    session_title       VARCHAR(256)    DEFAULT NULL            COMMENT '会话标题（可取首条用户消息摘要）',
    dify_conversation_id VARCHAR(128)   DEFAULT NULL            COMMENT 'Dify 会话编号，用于多轮对话续聊',
    status              VARCHAR(16)     NOT NULL DEFAULT 'active'
                                                            COMMENT '会话状态：active（进行中）/ deleted（已删除，软删除）',
    last_message_time   DATETIME        DEFAULT NULL            COMMENT '最近一条消息时间',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (session_id),
    INDEX idx_user_id       (user_id),
    INDEX idx_status        (status),
    INDEX idx_dify_conversation (dify_conversation_id),
    INDEX idx_last_message  (last_message_time),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI咨询会话表';


-- -----------------------------------------------------------
-- 表6：ai_chat_messages（AI 咨询消息表）
-- 说明：存储每个会话中的用户提问和 AI 回答
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_chat_messages (
    message_id          INT             NOT NULL AUTO_INCREMENT  COMMENT '消息编号',
    session_id          INT             NOT NULL                COMMENT '所属会话编号',
    user_id             INT             NOT NULL                COMMENT '用户编号',
    user_message        TEXT            NOT NULL                COMMENT '用户发送的问题',
    ai_response         TEXT            DEFAULT NULL            COMMENT 'AI 医生回复内容',
    context_summary     TEXT            DEFAULT NULL            COMMENT '本轮传给 AI 的上下文摘要',
    call_status         VARCHAR(16)     NOT NULL DEFAULT 'success'
                        COMMENT '调用状态：success（成功）/ failed（失败）',
    error_message       TEXT            DEFAULT NULL            COMMENT '调用失败原因，成功时为空',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息创建时间',
    PRIMARY KEY (message_id),
    INDEX idx_session_id    (session_id),
    INDEX idx_user_id       (user_id),
    INDEX idx_call_status   (call_status),
    INDEX idx_create_time   (create_time),
    CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES ai_chat_sessions(session_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_message_user    FOREIGN KEY (user_id)    REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI咨询消息表';


-- ============================================================
-- 模块6：个性化生活方案模块
-- ============================================================

-- -----------------------------------------------------------
-- 表7：life_plans（个性化生活方案表）
-- 说明：存储 Dify 生成的个性化饮食、运动、作息方案，
--       每个用户同一时间只有一个 active 方案，旧方案置为 history
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS life_plans (
    plan_id             INT             NOT NULL AUTO_INCREMENT  COMMENT '方案编号',
    user_id             INT             NOT NULL                COMMENT '用户编号',
    profile_id          INT             DEFAULT NULL            COMMENT '生成方案时使用的健康档案编号',
    metric_id           INT             DEFAULT NULL            COMMENT '生成方案时使用的健康数据编号',
    assessment_id       INT             DEFAULT NULL            COMMENT '生成方案时使用的风险评估编号',
    plan_title          VARCHAR(256)    DEFAULT NULL            COMMENT '方案标题',
    plan_goal           VARCHAR(128)    DEFAULT NULL            COMMENT '方案目标（如：控糖、减重、改善饮食习惯）',
    input_summary       TEXT            DEFAULT NULL            COMMENT '调用 Dify 时传入的数据摘要，便于管理端查看依据',
    plan_json           JSON            DEFAULT NULL            COMMENT 'Dify 返回的完整生活方案 JSON（含饮食、运动、作息计划）',
    checkin_tasks_json  JSON            DEFAULT NULL            COMMENT '从方案中拆出的饮食/运动打卡任务，供模块8使用',
    summary             TEXT            DEFAULT NULL            COMMENT '方案简短摘要',
    status              VARCHAR(16)     NOT NULL DEFAULT 'active'
                        COMMENT '方案状态：active（当前生效）/ history（历史方案）',
    call_status         VARCHAR(16)     NOT NULL DEFAULT 'success'
                        COMMENT 'Dify 调用状态：success（成功）/ failed（失败）',
    error_message       TEXT            DEFAULT NULL            COMMENT '调用失败原因，成功时为空',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '方案生成时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (plan_id),
    INDEX idx_user_id       (user_id),
    INDEX idx_status        (status),
    INDEX idx_call_status   (call_status),
    INDEX idx_create_time   (create_time),
    CONSTRAINT fk_plan_user           FOREIGN KEY (user_id)       REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_plan_profile        FOREIGN KEY (profile_id)    REFERENCES patient_profiles(profile_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_plan_metric         FOREIGN KEY (metric_id)     REFERENCES health_metrics(metric_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_plan_assessment     FOREIGN KEY (assessment_id) REFERENCES risk_assessments(assessment_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个性化生活方案表';


-- ============================================================
-- 模块7：健康资讯与科普内容模块
-- ============================================================

-- -----------------------------------------------------------
-- 表8：articles（健康资讯文章表）
-- 说明：存储管理员发布和管理的糖尿病健康科普文章
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS articles (
    article_id          INT             NOT NULL AUTO_INCREMENT  COMMENT '文章编号',
    title               VARCHAR(256)    NOT NULL                COMMENT '文章标题',
    category            VARCHAR(32)     NOT NULL
                        COMMENT '分类：diet（饮食指导）/ exercise（运动指南）/ habit（日常习惯）/ science（糖尿病科普）/ complication（并发症预防）/ mistake（控糖误区）',
    cover_image         VARCHAR(512)    DEFAULT NULL            COMMENT '封面图地址',
    summary             TEXT            DEFAULT NULL            COMMENT '文章摘要',
    content             TEXT            DEFAULT NULL            COMMENT '文章正文',
    status              VARCHAR(16)     NOT NULL DEFAULT 'draft'
                        COMMENT '文章状态：draft（草稿）/ published（已上架）/ offline（已下架）',
    view_count          INT             NOT NULL DEFAULT 0      COMMENT '浏览量',
    is_recommended      TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否首页推荐：1 是 / 0 否',
    sort_order          INT             NOT NULL DEFAULT 0      COMMENT '排序值，数字越小越靠前',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (article_id),
    INDEX idx_status        (status),
    INDEX idx_category      (category),
    INDEX idx_recommended   (is_recommended),
    INDEX idx_sort_order    (sort_order),
    INDEX idx_update_time   (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康资讯文章表';


-- -----------------------------------------------------------
-- 表9：home_contents（首页内容表）
-- 说明：存储首页轮播图和 AI 医师展示卡片，
--       前端通过 GET /api/home 一次性获取
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS home_contents (
    content_id          INT             NOT NULL AUTO_INCREMENT  COMMENT '首页内容编号',
    content_type        VARCHAR(16)     NOT NULL
                        COMMENT '内容类型：banner（轮播图）/ ai_doctor_card（AI 医师卡片）',
    title               VARCHAR(256)    NOT NULL                COMMENT '标题（轮播图标题或 AI 医师卡片名称）',
    subtitle            VARCHAR(512)    DEFAULT NULL            COMMENT '副标题或简介',
    image_url           VARCHAR(512)    DEFAULT NULL            COMMENT '图片地址',
    link_type           VARCHAR(16)     NOT NULL DEFAULT 'none'
                        COMMENT '跳转类型：none（无跳转）/ article（文章）/ chat（AI 咨询）/ life_plan（生活方案）',
    link_value          VARCHAR(128)    DEFAULT NULL            COMMENT '跳转目标（文章ID / "chat" / 为空）',
    sort_order          INT             NOT NULL DEFAULT 0      COMMENT '排序值，数字越小越靠前',
    status              VARCHAR(16)     NOT NULL DEFAULT 'enabled'
                        COMMENT '启用状态：enabled（启用）/ disabled（禁用）',
    created_by          INT             DEFAULT NULL            COMMENT '创建或维护该内容的管理员编号',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (content_id),
    INDEX idx_content_type  (content_type),
    INDEX idx_status        (status),
    INDEX idx_sort_order    (sort_order),
    CONSTRAINT fk_home_admin FOREIGN KEY (created_by) REFERENCES users(user_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页内容表（轮播图 + AI医师卡片）';


-- ============================================================
-- 模块8：生活打卡与行为分析模块
-- ============================================================

-- -----------------------------------------------------------
-- 表10：checkin_records（打卡记录表）
-- 说明：存储用户每日饮食和运动打卡记录
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS checkin_records (
    checkin_id          INT             NOT NULL AUTO_INCREMENT  COMMENT '打卡记录编号',
    user_id             INT             NOT NULL                COMMENT '用户编号',
    plan_id             INT             DEFAULT NULL            COMMENT '对应的生活方案编号',
    task_type           VARCHAR(16)     NOT NULL
                        COMMENT '打卡类型：diet（饮食）/ exercise（运动）',
    task_name           VARCHAR(128)    NOT NULL                COMMENT '任务名称（如：饮食打卡、运动打卡）',
    status              VARCHAR(16)     NOT NULL DEFAULT 'pending'
                        COMMENT '打卡状态：pending（待完成）/ completed（已完成）/ missed（未完成）',
    note                TEXT            DEFAULT NULL            COMMENT '用户备注（可记录实际饮食或运动情况）',
    checkin_date        DATE            NOT NULL                COMMENT '打卡日期',
    completed_time      DATETIME        DEFAULT NULL            COMMENT '完成时间',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (checkin_id),
    INDEX idx_user_id       (user_id),
    INDEX idx_plan_id       (plan_id),
    INDEX idx_task_type     (task_type),
    INDEX idx_status        (status),
    INDEX idx_checkin_date  (checkin_date),
    CONSTRAINT fk_checkin_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_checkin_plan FOREIGN KEY (plan_id) REFERENCES life_plans(plan_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡记录表（饮食+运动）';


-- -----------------------------------------------------------
-- 表11：checkin_analysis（打卡行为分析表）
-- 说明：存储 Dify 打卡行为分析工作流的分析结果
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS checkin_analysis (
    analysis_id                 INT             NOT NULL AUTO_INCREMENT  COMMENT '分析记录编号',
    user_id                     INT             NOT NULL                COMMENT '用户编号',
    plan_id                     INT             DEFAULT NULL            COMMENT '对应生活方案编号',
    start_date                  DATE            NOT NULL                COMMENT '分析开始日期',
    end_date                    DATE            NOT NULL                COMMENT '分析结束日期',
    total_days                  INT             NOT NULL DEFAULT 0      COMMENT '统计周期总天数',
    diet_completion_count       INT             NOT NULL DEFAULT 0      COMMENT '饮食完成天数',
    exercise_completion_count   INT             NOT NULL DEFAULT 0      COMMENT '运动完成天数',
    completion_rate             DECIMAL(5,2)    DEFAULT NULL            COMMENT '总完成率（百分比，如 78.00 表示 78%）',
    habit_score                 INT             DEFAULT NULL            COMMENT '健康习惯评分',
    diet_summary                TEXT            DEFAULT NULL            COMMENT '饮食打卡完成情况总结（AI 生成）',
    exercise_summary            TEXT            DEFAULT NULL            COMMENT '运动打卡完成情况总结（AI 生成）',
    life_evaluation             TEXT            DEFAULT NULL            COMMENT '生活状态评价（AI 生成）',
    main_problems               JSON            DEFAULT NULL            COMMENT '主要问题列表（JSON 数组）',
    improvement_suggestions     JSON            DEFAULT NULL            COMMENT '改进建议列表（JSON 数组）',
    next_focus                  TEXT            DEFAULT NULL            COMMENT '下一阶段重点（AI 生成）',
    summary                     TEXT            DEFAULT NULL            COMMENT 'AI 分析整体总结',
    input_summary               TEXT            DEFAULT NULL            COMMENT '调用 Dify 时传入的数据摘要',
    call_status                 VARCHAR(16)     NOT NULL DEFAULT 'success'
                                COMMENT 'Dify 调用状态：success（成功）/ failed（失败）',
    error_message               TEXT            DEFAULT NULL            COMMENT '调用失败原因，成功时为空',
    create_time                 DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分析创建时间',
    PRIMARY KEY (analysis_id),
    INDEX idx_user_id       (user_id),
    INDEX idx_plan_id       (plan_id),
    INDEX idx_start_date    (start_date),
    INDEX idx_call_status   (call_status),
    CONSTRAINT fk_analysis_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_analysis_plan FOREIGN KEY (plan_id) REFERENCES life_plans(plan_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡行为分析表';


-- ============================================================
-- 跨模块：日志记录
-- ============================================================

-- -----------------------------------------------------------
-- 表12：api_call_logs（Dify / API 调用日志表）
-- 说明：记录每次调用 Dify 工作流或 DeepSeek 等外部服务的日志
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_call_logs (
    log_id              INT             NOT NULL AUTO_INCREMENT  COMMENT '日志编号',
    user_id             INT             DEFAULT NULL            COMMENT '触发调用的用户编号',
    service_type        VARCHAR(64)     NOT NULL                COMMENT '调用服务类型（如：risk_prediction / ai_chat / life_plan / checkin_analysis）',
    request_summary     TEXT            DEFAULT NULL            COMMENT '请求摘要',
    response_summary    TEXT            DEFAULT NULL            COMMENT '响应摘要',
    call_status         VARCHAR(16)     NOT NULL DEFAULT 'success'
                        COMMENT '调用状态：success（成功）/ failed（失败）',
    error_message       TEXT            DEFAULT NULL            COMMENT '调用失败原因，成功时为空',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
    PRIMARY KEY (log_id),
    INDEX idx_user_id       (user_id),
    INDEX idx_service_type  (service_type),
    INDEX idx_call_status   (call_status),
    INDEX idx_create_time   (create_time),
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API调用日志表';


-- -----------------------------------------------------------
-- 表13：operation_logs（管理员操作日志表）
-- 说明：记录管理员在各管理端功能的操作行为
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS operation_logs (
    log_id              INT             NOT NULL AUTO_INCREMENT  COMMENT '日志编号',
    user_id             INT             NOT NULL                COMMENT '操作的管理员编号',
    action              VARCHAR(64)     NOT NULL                COMMENT '操作动作（如：create_user / disable_user / publish_article）',
    target_type         VARCHAR(64)     DEFAULT NULL            COMMENT '操作目标类型（如：user / article / home_content）',
    target_id           INT             DEFAULT NULL            COMMENT '操作目标编号',
    detail              TEXT            DEFAULT NULL            COMMENT '操作详情（JSON 或描述文本）',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (log_id),
    INDEX idx_user_id       (user_id),
    INDEX idx_action        (action),
    INDEX idx_target_type   (target_type),
    INDEX idx_create_time   (create_time),
    CONSTRAINT fk_oplog_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志表';


-- -----------------------------------------------------------
-- 表14：system_logs（系统运行日志表）
-- 说明：记录系统级别的运行日志，便于排查服务器异常
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_logs (
    log_id              INT             NOT NULL AUTO_INCREMENT  COMMENT '日志编号',
    log_level           VARCHAR(16)     NOT NULL DEFAULT 'info'
                        COMMENT '日志级别：info（信息）/ warn（警告）/ error（错误）',
    module              VARCHAR(64)     DEFAULT NULL            COMMENT '产生日志的模块名称',
    message             TEXT            DEFAULT NULL            COMMENT '日志消息',
    detail              TEXT            DEFAULT NULL            COMMENT '日志详情（如异常堆栈等）',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志时间',
    PRIMARY KEY (log_id),
    INDEX idx_log_level     (log_level),
    INDEX idx_module        (module),
    INDEX idx_create_time   (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统运行日志表';


-- ============================================================
-- 初始化管理员账号
-- 说明：默认管理员账号用于首次登录管理端
--       密码需要在应用层通过 bcrypt/PBKDF2 等算法加密后插入
-- ============================================================
-- INSERT INTO users (username, password_hash, phone, email, role, status)
-- VALUES ('admin', '<加密后的密码哈希>', NULL, 'admin@example.com', 'admin', 'active');


-- ============================================================
-- 建表完成
-- ============================================================
-- 表汇总（共 14 张表）：
--   模块1：users
--   模块2：patient_profiles
--   模块3：health_metrics
--   模块4：risk_assessments
--   模块5：ai_chat_sessions, ai_chat_messages
--   模块6：life_plans
--   模块7：articles, home_contents
--   模块8：checkin_records, checkin_analysis
--   跨模块：api_call_logs, operation_logs, system_logs
-- ============================================================
