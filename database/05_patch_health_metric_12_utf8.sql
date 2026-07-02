-- Fix garbled diet_status / exercise_status on health_metrics #12 (li_ming, 2026-06-30)
-- Cause: record inserted with non-UTF8 client encoding; stored as literal '?'

UPDATE health_metrics
SET
    diet_status = '早餐加入鸡蛋和蔬菜，午餐注意少油，晚餐主食控制在半碗。',
    exercise_status = '近7天完成5次饭后步行，周末一次轻量活动。'
WHERE metric_id = 12
  AND user_id = 2;
