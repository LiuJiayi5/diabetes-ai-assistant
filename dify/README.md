# Dify AI 应用说明

Dify / DeepSeek 只用于真正需要 AI 的功能，不承担普通业务接口、认证、数据库读写或管理端接口。

## AI Workflow

- diabetes_risk_prediction_workflow：模块4，糖尿病风险预测
- personalized_life_plan_workflow：模块6，个性化生活方案生成
- checkin_behavior_analysis_workflow：模块8，生活打卡行为分析

## AI Agent

- diabetes_ai_doctor_agent：模块5，AI 医生咨询

## Knowledge Base

- diabetes_knowledge_base：糖尿病知识库

所有 Dify 调用都由 Spring Boot 后端的 DifyService / DifyClient 发起，前端不直接调用 Dify。
