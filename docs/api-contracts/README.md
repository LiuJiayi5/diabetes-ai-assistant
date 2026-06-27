# API 契约说明

当前接口以 Spring Boot Controller、DTO、VO 和前端 `frontend/src/api/` 封装为准。

后端统一前缀为 `/api`，主要模块包括：

- `/api/auth/*`：登录、注册、邮箱验证码、忘记密码
- `/api/profile/*`：患者健康档案
- `/api/health-metrics/*`：健康数据
- `/api/risk/*`：风险预测
- `/api/life-plans/*`：生活方案
- `/api/checkin/*`：打卡与分析
- `/api/ai-chat/*`：AI 咨询
- `/api/articles/*`、`/api/home/*`：健康资讯和首页内容
- `/api/admin/*`：管理端接口

如需生成正式接口文档，建议后续接入 OpenAPI / Swagger，并以当前 Controller 注解自动导出。

