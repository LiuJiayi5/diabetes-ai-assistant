-- Restore risk_assessment #8 demo text fields (do not run via PowerShell < redirect; use Python script)
USE diabetes_assistant;

UPDATE risk_assessments SET
  metric_id = 2,
  request_summary = '46岁男性，空腹血糖6.5mmol/L，餐后血糖9.2mmol/L，已开始减少含糖饮料。',
  response_result = '{"risk_level":"medium","risk_score":68}',
  risk_level = 'medium',
  risk_score = 68,
  diabetes_type_tendency = '2型糖尿病风险倾向',
  main_risk_factors = '["家族史", "腰围偏高"]',
  indicator_analysis = '空腹与餐后血糖较首次评估有所下降，干预初见成效。',
  health_advice = '继续饭后步行，优化午餐外卖选择。',
  medical_warning = '保持监测，避免反弹。',
  summary = '风险分数下降，说明近期干预方向正确。',
  call_status = 'success',
  create_time = '2026-06-18 09:00:00'
WHERE assessment_id = 8 AND user_id = 2;
