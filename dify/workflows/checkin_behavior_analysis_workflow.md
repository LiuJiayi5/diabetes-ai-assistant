# checkin_behavior_analysis_workflow

## 名称

checkin_behavior_analysis_workflow

## 对应模块

模块8：生活打卡行为分析

## 输入字段摘要

- userId
- period
- checkinRecords
- completionRate
- lifePlanSummary
- healthMetricSummary

## 输出字段摘要

- habitScore
- dietSummary
- exerciseSummary
- lifeEvaluation
- mainProblems
- improvementSuggestions
- nextFocus
- summary

## 由哪个后端 Service 调用

- CheckinServiceImpl 后续通过 DifyService.callCheckinAnalysis 调用

## 后续 DSL 文件 TODO

- dify/workflows/exports/checkin_behavior_analysis_workflow.yml
