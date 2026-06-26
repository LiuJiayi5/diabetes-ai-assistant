-- Module 5 dev seed data for AI doctor consultation.
-- Development/local use only. Safe to run repeatedly.

INSERT INTO users (username, password_hash, phone, email, role, status)
VALUES
('dev_ai_chat_patient', 'dev_only_password_hash', '18800000501', 'dev_ai_chat_patient@example.com', 'patient', 'active'),
('dev_ai_chat_empty', 'dev_only_password_hash', '18800000502', 'dev_ai_chat_empty@example.com', 'patient', 'active'),
('dev_ai_chat_admin', 'dev_only_password_hash', '18800000500', 'dev_ai_chat_admin@example.com', 'admin', 'active')
ON DUPLICATE KEY UPDATE role = VALUES(role), status = 'active';

SET @patient_id := (SELECT user_id FROM users WHERE username = 'dev_ai_chat_patient');
SET @empty_id := (SELECT user_id FROM users WHERE username = 'dev_ai_chat_empty');

DELETE FROM api_call_logs
WHERE service_type = 'ai_chat'
  AND request_summary LIKE 'dev_ai_chat_seed:%';

DELETE FROM ai_chat_messages
WHERE user_id IN (@patient_id, @empty_id);

DELETE FROM ai_chat_sessions
WHERE user_id IN (@patient_id, @empty_id);

INSERT INTO ai_chat_sessions (user_id, session_title, dify_conversation_id, status, last_message_time, create_time, update_time)
VALUES
(@patient_id, 'Explain recent check-in analysis', 'dev-ai-chat-seed-conversation-success', 'active', NOW(), NOW(), NOW()),
(@patient_id, 'Dify fallback demo', NULL, 'active', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(@empty_id, 'No context consultation', NULL, 'active', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR));

SET @success_session_id := (
    SELECT session_id FROM ai_chat_sessions
    WHERE user_id = @patient_id AND dify_conversation_id = 'dev-ai-chat-seed-conversation-success'
    LIMIT 1
);
SET @failed_session_id := (
    SELECT session_id FROM ai_chat_sessions
    WHERE user_id = @patient_id AND session_title = 'Dify fallback demo'
    LIMIT 1
);
SET @empty_session_id := (
    SELECT session_id FROM ai_chat_sessions
    WHERE user_id = @empty_id AND session_title = 'No context consultation'
    LIMIT 1
);

INSERT INTO ai_chat_messages (session_id, user_id, user_message, ai_response, context_summary, call_status, error_message, create_time)
VALUES
(@success_session_id, @patient_id,
 'Please explain my recent check-in analysis',
 'Your diet check-ins have started, but exercise continuity is still weak. Start with a 15-minute walk after meals.',
 JSON_OBJECT('source','dev_ai_chat_seed','scenario','success_with_checkin_context'),
 'success', NULL, NOW()),
(@failed_session_id, @patient_id,
 'What happens when Dify is unavailable?',
 'The AI doctor is temporarily unavailable. Please try again later and record glucose, diet, exercise and symptoms first.',
 JSON_OBJECT('source','dev_ai_chat_seed','scenario','fallback_failed_call'),
 'failed', 'Dify chat request failed: HTTP 503', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(@empty_session_id, @empty_id,
 'Can I ask without a health profile?',
 'Yes. When information is insufficient, I will give general safe advice and remind you to complete health data.',
 JSON_OBJECT('source','dev_ai_chat_seed','scenario','no_context_user'),
 'success', NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR));

INSERT INTO api_call_logs (user_id, service_type, request_summary, response_summary, call_status, error_message, create_time)
VALUES
(@patient_id, 'ai_chat', 'dev_ai_chat_seed: explain recent check-in analysis', 'AI doctor answered with check-in context', 'success', NULL, NOW()),
(@patient_id, 'ai_chat', 'dev_ai_chat_seed: Dify unavailable fallback', 'Fallback answer returned', 'failed', 'Dify chat request failed: HTTP 503', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(@empty_id, 'ai_chat', 'dev_ai_chat_seed: no context user asks question', 'AI doctor answered with empty-context guidance', 'success', NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR));
