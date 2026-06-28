# Dify AI 应用说明

本项目的 AI 能力由 Spring Boot 后端统一调用 Dify，前端不直接调用 Dify。Dify 只负责需要大模型生成或推理的部分，不承担注册登录、普通 CRUD、管理端查询或数据库写入。

## Workflow / Agent

- 风险预测 Workflow：`dify/workflows/diabetes_risk_prediction_workflow.yml`
- 个性化生活方案 Workflow：`dify/workflows/personalized_life_plan_workflow.yml`
- 打卡行为分析 Workflow：`dify/workflows/checkin_behavior_analysis_workflow.md`
- AI 医生 Agent：`dify/agents/exports/diabetes_ai_doctor_agent.yml`

## 环境变量

```text
DIFY_BASE_URL=http://localhost/v1
DIFY_RISK_PREDICT_API_KEY=风险预测工作流 Key
DIFY_LIFE_PLAN_API_KEY=生活方案工作流 Key
DIFY_CHECKIN_ANALYSIS_API_KEY=打卡分析工作流 Key
DIFY_AI_DOCTOR_API_KEY=AI 医生 Agent Key
```

`DIFY_RISK_PREDICTION_API_KEY` 作为旧变量名兼容，推荐使用 `DIFY_RISK_PREDICT_API_KEY`。

## 专家身份

AI 医生支持多个专家身份。后端会把专家姓名、职称、科室、专长、persona 和开场白传给 Agent，使其能够回答“你是谁、擅长什么、当前以什么专家身份回答”。

## 知识库

知识库资料位于 `dify/knowledge-base/docs/`，可按 Dify 知识库导入流程使用。

风险预测工作流（`diabetes_risk_prediction_workflow`）已增加「知识检索」节点。导入 DSL 后请在 Dify 工作流编辑器中为该节点绑定 `diabetes_knowledge_base` 并重新发布。

