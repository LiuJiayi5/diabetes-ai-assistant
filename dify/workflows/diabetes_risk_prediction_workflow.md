# diabetes_risk_prediction_workflow

## 名称

diabetes_risk_prediction_workflow

## 对应模块

模块4：糖尿病风险预测

## 调用方式

Spring Boot `RiskServiceImpl` → `DifyService.callRiskPrediction` → Dify `/workflows/run`。

## 开始节点输入字段

这些字段需要和后端 `RiskServiceImpl.buildDifyInputs` 保持一致：

| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | string/number | 用户 ID |
| age | number | 年龄 |
| gender | string | 性别 |
| height_cm | number | 身高 cm |
| weight_kg | number | 体重 kg |
| bmi | number | BMI |
| waist_cm | number | 腰围 cm |
| systolic_bp | number | 收缩压 |
| diastolic_bp | number | 舒张压 |
| fasting_glucose | number | 空腹血糖 |
| postprandial_glucose | number | 餐后血糖 |
| hba1c | number | 糖化血红蛋白 |
| family_history | string | 家族病史 |
| chronic_history | string | 慢病史 |
| diet_status | string | 饮食状态 |
| exercise_status | string | 运动状态 |
| profile_summary | string | 健康档案摘要 |
| latest_metric | string | 最新健康指标摘要 |

## 输出字段

本地并列版 YAML 的输出节点变量为 `risk_result`，和后端
`DifyService.callRiskPrediction` 的优先读取字段一致。`risk_result`
内容应为纯 JSON 字符串，不要 markdown 代码块：

```json
{
  "risk_level": "low/medium/high",
  "risk_score": 0,
  "diabetes_type_tendency": "类型倾向描述",
  "main_risk_factors": ["因素1", "因素2"],
  "indicator_analysis": "指标分析",
  "health_advice": "健康建议",
  "medical_warning": "就医提醒",
  "summary": "一句话总结"
}
```

## DSL 文件

- 本地当前统一导入位置：`dify/workflows/diabetes_risk_prediction_workflow.yml`
- ljy 分支原始导出位置：`dify/workflows/exports/diabetes_risk_prediction_workflow.yml`

`exports` 下文件保留远端原始导出；根目录并列版已将输出变量名调整为
`risk_result`，更适配当前后端。

## 后端配置

```yaml
dify:
  risk-predict-api-key: ${DIFY_RISK_PREDICT_API_KEY:your_risk_predict_api_key}
```
