# content_recommendation_workflow

知识库增强型健康科普推荐工作流。该工作流用于把患者画像、当前生活方案、自动复盘结果和候选文章知识标签结合起来，生成可解释的个性化科普推荐理由。

## 调用位置

后端 `ArticleRecommendationServiceImpl` 会优先调用：

```text
DifyService.callContentRecommendation -> Dify /workflows/run
```

如果未配置 `DIFY_CONTENT_RECOMMENDATION_API_KEY` 或 Dify 调用失败，后端会自动退回本地规则引擎，保证演示和联调稳定。

## 输入变量

```text
scenario                  推荐场景：home / life_plan / intervention_review / article_detail
patient_profile           后端聚合出的患者教育画像 JSON
latest_plan_summary       当前生活方案摘要
latest_review_summary     最新自动复盘解释
candidate_articles        候选文章列表，包含 article_id/title/category/summary/tags
```

## 工作流设计

1. **输入校验**
   检查 `patient_profile`、`candidate_articles` 是否存在。若候选文章为空，直接返回空推荐。

2. **检索查询构造**
   把风险分层、指标异常、方案任务和复盘问题压缩为检索查询，例如：

   ```text
   餐后血糖偏高 饭后步行 运动完成率偏低 控糖餐盘 糖尿病生活方式干预
   ```

3. **知识库检索**
   绑定 `diabetes_knowledge_base`，检索糖尿病饮食、运动、血糖监测、并发症预防、安全边界等材料。

4. **推荐理由生成**
   LLM 根据患者画像、方案/复盘上下文、候选文章标签和知识库片段，输出每篇推荐的解释。

5. **结构化校验**
   输出必须是 JSON，且每条记录包含 `article_id`、`reason`、`knowledge_signal`、`score_boost`。

## 输出格式

```json
{
  "recommendations": [
    {
      "article_id": 6,
      "reason": "当前方案包含餐后步行任务，且近期餐后血糖仍需关注。知识库建议餐后轻中等强度活动有助于改善餐后血糖波动，因此推荐先阅读这篇内容。",
      "knowledge_signal": "餐后轻运动与餐后血糖波动管理",
      "score_boost": 18
    }
  ]
}
```

## 创新点表达

该工作流让健康资讯不再只是静态文章列表，而是成为生活干预闭环的一部分：

- 从患者画像中识别教育需求。
- 从生活方案中识别当前执行任务。
- 从自动复盘中识别行为偏差。
- 从知识库中检索糖尿病科普和安全边界依据。
- 输出可解释推荐理由，并由后端记录推荐批次与阅读行为。

