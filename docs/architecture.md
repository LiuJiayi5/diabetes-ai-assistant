# 系统架构

糖尿病预防智能助手采用前后端分离架构：Vue 3 前端负责患者端和管理端交互，Spring Boot 后端负责鉴权、业务接口、数据库读写和 Dify 调用，MySQL 保存业务数据和 AI 调用结果。

```text
Browser
  -> Vue 3 / Vite
  -> Spring Boot REST API
  -> MySQL
  -> Dify Workflow / Agent
```

## 前端

- 患者端：移动端容器体验，包含首页、档案、健康数据、风险预测、生活方案、打卡、AI 医生、健康资讯和个人中心。
- 管理端：桌面后台体验，包含用户、健康档案、健康数据、风险评估、生活方案、打卡、AI 咨询、专家、资讯和首页内容管理。
- 状态管理：Pinia 保存登录状态、用户信息和部分模块缓存。
- UI：患者端主要使用自定义移动端样式与 Vant，管理端使用 Element Plus 风格组件。

## 后端

- `common`：统一响应、异常处理、JWT、当前用户、分页工具等公共能力。
- `modules/user`：注册登录、邮箱验证码、忘记密码、用户管理。
- `modules/profile`：患者健康档案。
- `modules/healthmetric`：健康指标记录。
- `modules/risk`：风险预测请求、解析和结果保存。
- `modules/lifeplan`：个性化生活方案和任务生成。
- `modules/checkin`：打卡记录、统计、分析和管理端查询。
- `modules/aichat`：AI 专家身份、会话、消息和管理端日志。
- `modules/content`：健康资讯、首页轮播和内容管理。
- `modules/dify`：Dify Client、Workflow / Agent 调用封装。

## 数据库

数据库脚本统一在 `database/`：

- `01_init_schema.sql`：从零创建 schema、表、索引和外键。
- `02_seed_demo_data.sql`：写入干净演示数据。

核心关系：用户一对一健康档案，用户一对多健康数据、风险评估、生活方案、打卡记录和 AI 会话；生活方案可关联档案、指标和风险评估；AI 会话必须绑定用户，并可绑定一个专家身份。

## AI 调用边界

前端不直接调用 Dify。后端负责收集用户上下文、专家身份、健康档案、指标和历史结果，再调用对应 Workflow / Agent，并将响应落库。Dify 不承担注册登录、管理端 CRUD 或普通数据库写入。

