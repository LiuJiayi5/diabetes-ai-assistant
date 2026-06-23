# personalized_life_plan_workflow

## 名称

personalized_life_plan_workflow

## 对应模块

模块6：个性化生活方案生成

## 输入字段摘要

- userId
- profileSummary
- healthMetricSummary
- riskSummary
- userGoal

## 输出字段摘要

- planTitle
- planGoal
- dietPlanJson
- exercisePlanJson
- dailyScheduleJson
- checkinTasksJson
- healthTipsJson
- summary

## 由哪个后端 Service 调用

- LifePlanServiceImpl 后续通过 DifyService.callLifePlan 调用

## 后续 DSL 文件 TODO

- dify/workflows/exports/personalized_life_plan_workflow.yml
