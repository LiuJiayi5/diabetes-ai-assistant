# diabetes_risk_prediction_workflow

## 名称

diabetes_risk_prediction_workflow

## 对应模块

模块4：糖尿病风险预测

## 调用方式

Spring Boot `RiskServiceImpl` → `DifyService.callRiskPrediction` → `POST /v1/workflows/run`

## 开始节点输入字段（与后端 buildDifyInputs 一致）

| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | string | 用户 ID（Dify text-input 必须为字符串） |
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
| chronic_history | string | 慢性病史 |
| diet_status | string | 饮食状态（文本） |
| exercise_status | string | 运动状态（文本） |
| profile_summary | string | 档案摘要 |
| latest_metric | string | 最新指标摘要 |

## LLM 输出（text 字段 JSON）

| 字段 | 说明 |
|------|------|
| risk_level | low / medium / high |
| risk_score | 0–100 |
| diabetes_type_tendency | 类型倾向 |
| main_risk_factors | 字符串数组 |
| indicator_analysis | 指标分析 |
| health_advice | 健康建议 |
| medical_warning | 就医提醒 |
| summary | 一句话总结 |

## 工作流结构

```
开始 → LLM（关闭结构化输出，Prompt 要求纯 JSON）→ 输出（映射 text）
```

## DSL 文件

- 导出路径：`dify/workflows/exports/diabetes_risk_prediction_workflow.yml`
- 见 `exports/README.md` 导入说明

## 后端配置

```yaml
dify:
  base-url: http://localhost/v1
  risk-predict-api-key: app-xxxxxxxx   # 各环境自行填写，勿提交真实 Key
```
