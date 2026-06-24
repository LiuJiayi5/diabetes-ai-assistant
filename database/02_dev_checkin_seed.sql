-- 仅用于本地开发验证，可按需执行。
-- 用途：为模块8普通打卡接口准备一个患者用户和一个当前有效生活方案。
-- 不属于生产初始化数据，后续应由模块1登录注册和模块6生活方案生成真实数据替换。

USE diabetes_assistant;

INSERT INTO users (username, password_hash, phone, email, role, status)
VALUES ('dev_patient_checkin', 'dev_only_password_hash', NULL, 'dev_patient_checkin@example.com', 'patient', 'active')
ON DUPLICATE KEY UPDATE status = 'active';

SET @dev_user_id := (SELECT user_id FROM users WHERE username = 'dev_patient_checkin');

INSERT INTO life_plans (
    user_id,
    plan_title,
    plan_goal,
    plan_json,
    checkin_tasks_json,
    summary,
    status,
    call_status
)
SELECT
    @dev_user_id,
    '模块8本地验证生活方案',
    '控制血糖并保持规律运动',
    JSON_OBJECT('source', 'dev_checkin_seed'),
    JSON_ARRAY(
        JSON_OBJECT('task_type', 'diet', 'task_name', '饮食打卡', 'description', '记录今日饮食是否符合方案'),
        JSON_OBJECT('task_type', 'exercise', 'task_name', '运动打卡', 'description', '记录今日运动完成情况')
    ),
    '仅用于模块8本地接口验证',
    'active',
    'success'
WHERE NOT EXISTS (
    SELECT 1 FROM life_plans
    WHERE user_id = @dev_user_id
      AND status = 'active'
);

SELECT @dev_user_id AS dev_user_id;
