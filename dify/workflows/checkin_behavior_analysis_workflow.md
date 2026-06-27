# checkin_behavior_analysis_workflow

## 用途

模块 8 的生活打卡行为分析工作流。后端通过 `DifyService.callCheckinAnalysis` 调用，用于把用户健康档案、最新健康数据、风险评估、当前生活方案、打卡记录和统计趋势转成结构化分析结果，保存到 `checkin_analysis`。

## 流程

1. **用户输入**：接收后端组装好的用户资料、方案、打卡明细、统计字段和安全规则。
2. **校验输入**：解析 JSON 字段，统一计算完成率、数据充分性、薄弱日期、风险提示和标准化上下文。
3. **是否分析**：输入足够时进入 LLM；缺少核心数据时直接返回失败输出。
4. **生成打卡分析**：DeepSeek 根据标准化上下文生成严格 JSON。
5. **校验结果**：清理代码块/多余文本，解析 JSON，补齐后端必需字段，并强制使用后端完成率。
6. **成功/失败输出**：统一返回 `success`、`analysis_result`、`completion_rate`、`error_message`、`missing_fields`、`input_summary`。

## 后端输入字段

保留旧字段，并扩展更丰富上下文：

- `user_id`
- `user_profile`
- `latest_health_data`
- `risk_result`
- `life_plan`
- `plan_title`
- `plan_goal`
- `plan_tasks_json`
- `checkin_records`
- `checkin_stats_json`
- `checkin_trend_json`
- `task_breakdown_json`
- `diet_completion_count`
- `exercise_completion_count`
- `total_days`
- `completion_rate`
- `start_date`
- `end_date`
- `user_notes`
- `safety_rules`

`checkin_records`、`checkin_stats_json`、`checkin_trend_json`、`task_breakdown_json`、`plan_tasks_json` 均为 JSON 字符串。

## 输出字段

End 节点输出：

- `success`
- `analysis_result`
- `completion_rate`
- `error_message`
- `missing_fields`
- `input_summary`

其中 `analysis_result` 是 JSON 字符串，后端解析并落库。必须包含：

- `completion_rate`
- `diet_summary`
- `exercise_summary`
- `habit_score`
- `life_evaluation`
- `main_problems`
- `improvement_suggestions`
- `next_focus`
- `summary`

后端仍以自身计算出的完成率为准保存，工作流中的 `completion_rate` 主要用于提示词和排错。
