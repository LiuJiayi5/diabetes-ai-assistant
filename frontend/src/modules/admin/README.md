# 管理端模块

## 模块 2/3/4 管理页（已实现）

| 路由 | 页面 |
|------|------|
| `/admin/profiles` | 健康档案列表（筛选 + 分页） |
| `/admin/profiles/:userId` | 档案详情 |
| `/admin/health-metrics` | 健康数据列表（筛选 + 分页） |
| `/admin/risk-assessments` | 风险评估列表（筛选 + 分页） |
| `/admin/risk-assessments/:assessmentId` | 评估详情 |

## API

- `src/api/profile.js` → `adminListProfiles` / `adminGetProfileDetail`
- `src/api/healthMetric.js` → `adminListMetrics`
- `src/api/riskAssessment.js` → `adminListAssessments` / `adminGetRiskDetail`

## 开发环境鉴权

访问 `/admin/*` 时，`request.js` 自动附加：

- `X-Dev-User-Id: 2`（可通过 `VITE_DEV_ADMIN_USER_ID` 覆盖）
- `X-Dev-User-Role: admin`

登录模块就绪后改为 JWT + admin 角色校验。

## 其他管理模块

用户、内容、打卡等仍为 `AdminModuleEntryView` 占位，由对应模块同学实现。
