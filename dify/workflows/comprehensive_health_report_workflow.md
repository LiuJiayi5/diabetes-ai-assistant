# 糖尿病健康管理综合报告工作流

## 用途

该工作流用于“健康报告 / 就医报告”模块。后端会聚合用户健康档案、近期健康指标、Dify 风险评估、生活方案、打卡分析、打卡记录和 AI 咨询摘要，然后调用该工作流生成结构化综合报告结论。

## 导入文件

- 工作流：`dify/workflows/comprehensive_health_report_workflow.yml`
- 测试输入：`dify/workflows/test_comprehensive_health_report_input.json`

导入 Dify 后，需要在后端配置：

```bash
DIFY_COMPREHENSIVE_REPORT_API_KEY=你的综合报告工作流 API Key
```

未配置该 key 时，后端会自动使用本地增强规则兜底，不会影响报告生成和扫码演示。

## 输出字段

后端优先读取工作流输出的 `report_result`。该字段是一个 JSON 字符串，核心结构如下：

```json
{
  "patient_report": {},
  "doctor_report": {},
  "metric_trend": {},
  "evidence_chain": [],
  "data_gaps": [],
  "safety_warnings": [],
  "next_cycle_adjustment": {},
  "summary": ""
}
```

## 答辩表述

可以表述为：

> 系统新增综合健康管理报告工作流，将风险预测、生活方案、打卡分析和 AI 咨询摘要统一整理为患者版和医生版双视角报告，并通过证据链、数据缺口和下一周期干预调整增强报告的可解释性与连续健康管理能力。
