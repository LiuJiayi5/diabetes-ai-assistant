-- Development-only seed data for module 8 admin pages.
-- Safe to re-run: rows are keyed by stable dev usernames and fixed date ranges.

USE diabetes_assistant;

INSERT INTO users (username, password_hash, phone, email, role, status)
VALUES
('dev_admin_checkin', 'dev_only_password_hash', '18800000000', 'dev_admin_checkin@example.com', 'admin', 'active'),
('dev_checkin_regular', 'dev_only_password_hash', '18800000001', 'regular@example.com', 'patient', 'active'),
('dev_checkin_partial', 'dev_only_password_hash', '18800000002', 'partial@example.com', 'patient', 'active'),
('dev_checkin_inactive', 'dev_only_password_hash', '18800000003', 'inactive@example.com', 'patient', 'active')
ON DUPLICATE KEY UPDATE role = VALUES(role), status = 'active';

SET @admin_id := (SELECT user_id FROM users WHERE username = 'dev_admin_checkin');
SET @regular_id := (SELECT user_id FROM users WHERE username = 'dev_checkin_regular');
SET @partial_id := (SELECT user_id FROM users WHERE username = 'dev_checkin_partial');
SET @inactive_id := (SELECT user_id FROM users WHERE username = 'dev_checkin_inactive');

INSERT INTO life_plans (user_id, plan_title, plan_goal, plan_json, checkin_tasks_json, summary, status, call_status)
SELECT user_id,
       CONCAT('module8-admin-demo-plan-', username),
       'stable glucose control and regular exercise',
       JSON_OBJECT('source', 'dev_checkin_admin_seed'),
       JSON_ARRAY(
           JSON_OBJECT('task_type', 'diet', 'task_name', 'diet check-in', 'description', 'record diet execution'),
           JSON_OBJECT('task_type', 'exercise', 'task_name', 'exercise check-in', 'description', 'record exercise execution')
       ),
       CONCAT(username, ' module8 admin demo plan'),
       'active',
       'success'
FROM users
WHERE username IN ('dev_checkin_regular', 'dev_checkin_partial', 'dev_checkin_inactive')
  AND NOT EXISTS (
      SELECT 1 FROM life_plans lp
      WHERE lp.user_id = users.user_id
        AND lp.status = 'active'
        AND lp.plan_title LIKE 'module8-admin-demo-plan-%'
  );

SET @regular_plan := (SELECT plan_id FROM life_plans WHERE user_id = @regular_id AND status = 'active' ORDER BY create_time DESC LIMIT 1);
SET @partial_plan := (SELECT plan_id FROM life_plans WHERE user_id = @partial_id AND status = 'active' ORDER BY create_time DESC LIMIT 1);
SET @inactive_plan := (SELECT plan_id FROM life_plans WHERE user_id = @inactive_id AND status = 'active' ORDER BY create_time DESC LIMIT 1);

DELETE FROM checkin_records
WHERE user_id IN (@regular_id, @partial_id, @inactive_id)
  AND checkin_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 14 DAY) AND CURDATE();

INSERT INTO checkin_records (user_id, plan_id, task_type, task_name, status, note, checkin_date, completed_time)
SELECT @regular_id, @regular_plan, 'diet', 'diet check-in', 'completed', 'diet followed with enough vegetables', DATE_SUB(CURDATE(), INTERVAL n DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL n DAY), INTERVAL 20 HOUR)
FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) days
UNION ALL
SELECT @regular_id, @regular_plan, 'exercise', 'exercise check-in',
       CASE WHEN n IN (1, 5) THEN 'missed' ELSE 'completed' END,
       CASE WHEN n IN (1, 5) THEN 'missed because of overtime' ELSE '30-minute walk after dinner' END,
       DATE_SUB(CURDATE(), INTERVAL n DAY),
       CASE WHEN n IN (1, 5) THEN NULL ELSE DATE_ADD(DATE_SUB(CURDATE(), INTERVAL n DAY), INTERVAL 21 HOUR) END
FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) days
UNION ALL
SELECT @partial_id, @partial_plan, 'diet', 'diet check-in',
       CASE WHEN n IN (0, 2, 6) THEN 'completed' ELSE 'missed' END,
       CASE WHEN n IN (0, 2, 6) THEN 'light dinner recorded' ELSE 'diet record missing' END,
       DATE_SUB(CURDATE(), INTERVAL n DAY),
       CASE WHEN n IN (0, 2, 6) THEN DATE_ADD(DATE_SUB(CURDATE(), INTERVAL n DAY), INTERVAL 19 HOUR) ELSE NULL END
FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) days
UNION ALL
SELECT @partial_id, @partial_plan, 'exercise', 'exercise check-in',
       CASE WHEN n = 2 THEN 'completed' WHEN n = 0 THEN 'pending' ELSE 'missed' END,
       CASE WHEN n = 2 THEN '20-minute indoor stretching' WHEN n = 0 THEN 'waiting for today update' ELSE 'exercise missing' END,
       DATE_SUB(CURDATE(), INTERVAL n DAY),
       CASE WHEN n = 2 THEN DATE_ADD(DATE_SUB(CURDATE(), INTERVAL n DAY), INTERVAL 20 HOUR) ELSE NULL END
FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) days
UNION ALL
SELECT @inactive_id, @inactive_plan, 'diet', 'diet check-in', 'missed', 'continuous diet records missing', DATE_SUB(CURDATE(), INTERVAL n DAY), NULL
FROM (SELECT 8 n UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14) days
UNION ALL
SELECT @inactive_id, @inactive_plan, 'exercise', 'exercise check-in', 'missed', 'continuous exercise records missing', DATE_SUB(CURDATE(), INTERVAL n DAY), NULL
FROM (SELECT 8 n UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14) days;

DELETE FROM checkin_analysis
WHERE user_id IN (@regular_id, @partial_id, @inactive_id)
  AND input_summary LIKE '%dev_checkin_admin_seed%';

INSERT INTO checkin_analysis (
    user_id, plan_id, start_date, end_date, total_days, diet_completion_count, exercise_completion_count,
    completion_rate, habit_score, diet_summary, exercise_summary, life_evaluation, main_problems,
    improvement_suggestions, next_focus, summary, input_summary, call_status, error_message, create_time
)
VALUES
(@regular_id, @regular_plan, DATE_SUB(CURDATE(), INTERVAL 6 DAY), CURDATE(), 7, 7, 5, 85.71, 86,
 'Diet check-ins are stable and complete.', 'Exercise has two missed days but remains acceptable.',
 'Behavior execution is stable in the recent period.', JSON_ARRAY('Exercise continuity needs attention'),
 JSON_ARRAY('Keep a fixed walking time after dinner', 'Plan low-intensity exercise on weekends'), 'Maintain diet stability and improve exercise continuity.',
 'Recent 7-day completion rate is high.', JSON_OBJECT('source','dev_checkin_admin_seed','scenario','success_regular'), 'success', NULL, NOW()),
(@partial_id, @partial_plan, DATE_SUB(CURDATE(), INTERVAL 6 DAY), CURDATE(), 7, 3, 1, 28.57, 48,
 'Diet records are intermittent.', 'Exercise execution is clearly insufficient.',
 'Behavior execution is weak and needs attention.', JSON_ARRAY('Poor continuity', 'Low exercise completion rate'),
 JSON_ARRAY('Complete at least one diet record every day', 'Restart with a 10-minute walk'), 'Recover check-in continuity first.',
 'Recent completion rate is low and should be monitored.', JSON_OBJECT('source','dev_checkin_admin_seed','scenario','success_partial'), 'success', NULL, NOW()),
(@inactive_id, @inactive_plan, DATE_SUB(CURDATE(), INTERVAL 14 DAY), DATE_SUB(CURDATE(), INTERVAL 8 DAY), 7, 0, 0, 0.00, NULL,
 '', '', '', JSON_ARRAY(),
 JSON_ARRAY(), '', 'AI analysis failed because valid check-in samples are insufficient.',
 JSON_OBJECT('source','dev_checkin_admin_seed','scenario','failed_inactive'), 'failed', 'Insufficient valid check-in samples for reliable behavior analysis.', NOW());

DELETE FROM api_call_logs
WHERE service_type = 'checkin_analysis'
  AND request_summary LIKE '%dev_checkin_admin_seed%';

INSERT INTO api_call_logs (user_id, service_type, request_summary, response_summary, call_status, error_message, create_time)
VALUES
(@regular_id, 'checkin_analysis', 'dev_checkin_admin_seed: regular user 7-day analysis input', 'completion_rate=85.71; AI analysis success', 'success', NULL, NOW()),
(@partial_id, 'checkin_analysis', 'dev_checkin_admin_seed: partial user 7-day analysis input', 'completion_rate=28.57; AI analysis success', 'success', NULL, NOW()),
(@inactive_id, 'checkin_analysis', 'dev_checkin_admin_seed: inactive user insufficient records', 'AI analysis failed', 'failed', 'Insufficient valid check-in samples for reliable behavior analysis.', NOW());

SELECT @admin_id AS dev_admin_user_id, @regular_id AS regular_user_id, @partial_id AS partial_user_id, @inactive_id AS inactive_user_id;
