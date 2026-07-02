# 模块4：糖尿病风险预测

## 功能

- 患者端：指标录入、自动/手动风险预测、评估历史、评估详情
- 管理端：后端 admin 列表/详情 API + 前端 `/admin/risk-assessments`

## 路由

| 路径 | 页面 |
|------|------|
| `/app/risk` | 风险预测主页（录入 + 评估） |
| `/app/risk/history` | 评估记录 |
| `/app/risk/:assessmentId` | 评估详情 |

## API

- 前端：`src/api/riskAssessment.js`
- 后端：`/api/risk/*`

## Dify

- 工作流：`diabetes_risk_prediction_workflow`
- DSL：`dify/workflows/exports/diabetes_risk_prediction_workflow.yml`（导出后提交）
- 配置：`dify.risk-predict-api-key`（各环境自行填写 `app-...` Key）
- 未配置 Key 或 placeholder 时后端使用 dev mock

## 整合说明

- 预测前需有健康档案 + 最新健康数据，否则返回 400
- LLM 返回可能含 `` 推理块，后端 `RiskResultParser` 已处理
- Dify 开始节点 `user_id` 必须为 string 类型
