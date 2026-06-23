# diabetes_ai_doctor_agent

## 名称

diabetes_ai_doctor_agent

## 对应模块

模块5：AI 医生咨询

## 输入字段摘要

- message
- conversationId
- userContext
- knowledgeBaseContext

## 输出字段摘要

- answer
- conversationId
- messageId
- safetyNotice

## 由哪个后端 Service 调用

- AiChatServiceImpl 后续通过 DifyService.callAiDoctor 调用

## 安全边界

- 不做最终诊断
- 不开处方
- 不替代线下医生
- 高风险或异常情况提示线下就医

## 关联知识库

- diabetes_knowledge_base

## 后续 DSL / 配置文件 TODO

- dify/agents/exports/diabetes_ai_doctor_agent.yml
