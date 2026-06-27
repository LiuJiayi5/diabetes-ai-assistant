# 模块3：健康数据

## 功能

- 患者端：健康数据录入（在风险预测页）、历史列表
- 管理端：后端 admin 列表 API + 前端 `/admin/health-metrics`

## 路由

| 路径 | 页面 |
|------|------|
| `/app/health-metric/history` | 健康数据历史 |
| `/app/risk` | 风险预测页内嵌录入（与模块4联动） |

## API

- 前端：`src/api/healthMetric.js`
- 后端：`/api/health-metric/*`

## Dify

不涉及。

## 整合说明

- 录入后可触发模块4自动评估（需先有健康档案）
- 个人中心建议链接至 `/app/health-metric/history`
