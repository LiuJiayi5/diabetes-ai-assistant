# 模块2：健康档案

## 功能

- 患者端：档案新增、修改、查询
- 管理端：后端 admin 列表/详情 API 已实现（前端 admin 页为占位）

## 路由

| 路径 | 页面 |
|------|------|
| `/app/profile` | 健康档案录入/编辑 |

## API

- 前端：`src/api/profile.js`
- 后端：`/api/profile/*`

## Dify

不涉及。

## 整合说明

- 开发环境鉴权：`X-Dev-User-Id`（见 `request.js`），登录模块就绪后改为 JWT
- 个人中心入口由模块1负责，建议链接至 `/app/profile`
