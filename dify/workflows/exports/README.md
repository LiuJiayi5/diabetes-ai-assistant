# Dify 工作流 DSL 导出目录

将 Dify 控制台导出的 DSL 文件放在此目录，便于团队导入与版本管理。

## 文件命名

| 文件 | 对应应用 |
|------|----------|
| `diabetes_risk_prediction_workflow.yml` | 模块4 糖尿病风险预测 |

## 导出步骤（Dify 控制台）

1. 打开对应 Workflow 应用
2. 右上角 **导出 DSL**
3. 保存为本目录下的 yml 文件
4. 提交到 Git（DSL 内不含 Dify API Key，可安全提交）

## 导入步骤（队友 / 新环境）

1. Dify → **创建应用** → **导入 DSL**
2. 选择本目录下的 yml 文件
3. 配置 DeepSeek 模型并 **发布**
4. **访问 API** 生成 `app-...` Key，填入后端 `application-dev.yml` 的 `dify.risk-predict-api-key`

## 与后端对齐的输入变量（snake_case）

开始节点需包含（均建议非必填，避免 null 校验失败）：

`user_id`（字符串）、`age`、`gender`、`height_cm`、`weight_kg`、`bmi`、`waist_cm`、`systolic_bp`、`diastolic_bp`、`fasting_glucose`、`postprandial_glucose`、`hba1c`、`family_history`、`chronic_history`、`diet_status`、`exercise_status`、`profile_summary`、`latest_metric`

输出节点映射 LLM 的 `text`（JSON，含 `risk_level`、`risk_score` 等字段）。
