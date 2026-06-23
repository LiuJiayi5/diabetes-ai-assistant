# diabetes_risk_prediction_workflow

## 名称

diabetes_risk_prediction_workflow

## 对应模块

模块4：糖尿病风险预测

## 输入字段摘要

- userId
- profileSummary
- latestMetric
- metricHistorySummary
- predictionPayload

## 输出字段摘要

- riskLevel
- riskScore
- diabetesTypeTendency
- mainRiskFactors
- indicatorAnalysis
- healthAdvice
- medicalWarning
- summary

## 由哪个后端 Service 调用

- RiskServiceImpl 后续通过 DifyService.callRiskPrediction 调用

## 后续 DSL 文件 TODO

- dify/workflows/exports/diabetes_risk_prediction_workflow.yml
