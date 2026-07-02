-- Fix li_ming risk trend demo dates/scores only (preserve detail columns)
USE diabetes_assistant;

DELETE FROM risk_assessments
WHERE user_id = 2
  AND assessment_id NOT IN (1, 7, 8);

UPDATE risk_assessments SET
  metric_id = 1,
  risk_score = 72,
  create_time = '2026-06-12 09:00:00'
WHERE assessment_id = 7 AND user_id = 2;

UPDATE risk_assessments SET
  metric_id = 2,
  risk_score = 68,
  create_time = '2026-06-18 09:00:00'
WHERE assessment_id = 8 AND user_id = 2;

UPDATE risk_assessments SET
  risk_score = 64,
  create_time = '2026-06-26 09:00:00'
WHERE assessment_id = 1 AND user_id = 2;
