# 架构说明

## 总体架构

```text
Vue 3 前端
  -> Axios request.js
  -> Spring Boot REST API
  -> Service / Contract / Mapper
  -> MySQL

Spring Boot AI Service
  -> Dify Workflow / Agent
  -> DeepSeek / Knowledge Base
```

## 前端如何调用后端

前端只调用 Spring Boot 提供的 `/api/**` 接口。`src/api/request.js` 统一配置 `VITE_API_BASE_URL`、Authorization Token、响应处理和 401 处理。

## 后端如何调用数据库

Spring Boot 通过 MyBatis-Plus 访问 MySQL。各模块只能访问本模块 Mapper、Entity 和私有 DTO。

## 后端如何调用 Dify

只有后端的 DifyService / DifyClient 能调用 Dify。前端不直接调用 Dify，也不持有 Dify API Key。

## 哪些模块调用 Dify

- 模块4：risk，调用 diabetes_risk_prediction_workflow
- 模块5：aichat，调用 diabetes_ai_doctor_agent
- 模块6：lifeplan，调用 personalized_life_plan_workflow
- 模块8：checkin，调用 checkin_behavior_analysis_workflow

## 哪些模块不调用 Dify

- user
- profile
- healthmetric
- content
- checkin 的普通打卡功能

## 模块间通过 contract 接口互通

模块之间只能调用其他模块的 `contract` 包下的 Java Interface 和 DTO。contract DTO 不等于数据库 Entity。

## 禁止跨模块直接调用

- Controller
- ServiceImpl
- Mapper
- Entity
- 私有 DTO
