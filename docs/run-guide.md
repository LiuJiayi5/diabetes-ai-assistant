# 项目启动运行说明

本文档说明糖尿病预治智能助手在本地开发环境的启动方式。当前项目由 Vue 3 前端、Spring Boot 后端、MySQL 数据库和可选 Dify AI 服务组成。

## 环境要求

- JDK 17
- Maven 3.8+
- Node.js 18+，建议 20+
- npm
- MySQL 8.x
- 可选：Dify 服务，用于风险预测、生活方案、AI 咨询、打卡分析等 AI 能力

## 目录说明

```text
frontend/   Vue 3 + Vite 前端
backend/    Spring Boot 后端
database/   MySQL 建表和测试数据脚本
docs/       项目文档
```

## 数据库准备

1. 确认 MySQL 正在运行。
2. 默认开发配置连接：

```text
数据库：diabetes_assistant
用户：root
密码：123456
端口：3306
```

3. 初始化数据库：

```bash
mysql -uroot -p123456 < database/01_init_schema.sql
mysql -uroot -p123456 diabetes_assistant < database/02_test_data.sql
```

4. 如果已有数据，不要随意重复执行会清表的初始化脚本。需要重置演示环境时再执行。

## 后端启动

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://127.0.0.1:8080
```

健康检查：

```bash
curl http://127.0.0.1:8080/api/health
```

成功时返回 `code=200` 和 `status=ok`。

## 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://127.0.0.1:5173
```

前端开发环境接口地址在 `frontend/.env.development` 中配置：

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

## Dify 配置

普通业务不依赖 Dify。以下 AI 能力需要 Dify：

- 模块4 风险预测
- 模块5 AI 医生咨询
- 模块6 个性化生活方案
- 模块8 打卡分析

开发环境可通过环境变量配置：

```powershell
$env:DIFY_BASE_URL="http://127.0.0.1:8098/v1"
$env:DIFY_LIFE_PLAN_API_KEY="你的生活方案工作流 API Key"
$env:DIFY_RISK_PREDICT_API_KEY="你的风险预测工作流 API Key"
$env:DIFY_AI_DOCTOR_API_KEY="你的 AI 医生应用 API Key"
$env:DIFY_CHECKIN_ANALYSIS_API_KEY="你的打卡分析工作流 API Key"
```

设置后重新启动后端。后端启动日志会打印 Dify 配置状态，例如 `lifePlanKey=configured`。

## 访问入口和账号

患者端：

```text
登录页：http://127.0.0.1:5173/login
患者首页：http://127.0.0.1:5173/app/home
```

管理端：

```text
管理端登录：http://127.0.0.1:5173/admin/login
管理端首页：http://127.0.0.1:5173/admin/dashboard
```

本地演示常用账号以数据库测试数据为准。当前本地常用账号：

```text
患者：module6_patient / 123456
管理员：module6_admin / 123456
```

## 图片上传和访问

- 后端上传接口保存图片到 `backend/uploads/`。
- 浏览器访问路径为 `/uploads/**`，由 `WebMvcConfig` 映射到本地上传目录。
- `backend/uploads/` 被 `.gitignore` 忽略，换机演示时需要同步实际上传文件，或者在管理端重新上传图片。
- 患者头像、管理员头像、资讯封面、首页内容图、轮播图等均使用统一上传组件。
- 头像上传支持拖动定位和缩放裁剪。

## 常见问题

### 1. 前端提示 Network Error

优先检查：

- 后端 8080 是否启动。
- `frontend/.env.development` 中 `VITE_API_BASE_URL` 是否指向正确后端。
- 浏览器是否保留了旧 token，可退出后重新登录。
- 管理端接口是否使用管理员账号访问。患者账号访问 `/api/admin/**` 会返回 403。

当前后端已放行 CORS 预检 `OPTIONS` 请求，正常情况下跨端口访问不会再因为预检失败显示 `Network Error`。

### 2. 患者和管理员账号显示错乱

当前代码已按角色隔离 token：

- 患者 token：`diabetes_patient_jwt`
- 管理员 token：`diabetes_admin_jwt`

并且后端统一限制 `/api/admin/**` 只能由管理员 token 访问。如果浏览器中仍有历史缓存，退出登录或清理 localStorage 后重新登录。

### 3. Dify 连接失败

- 先确认 `DIFY_BASE_URL` 和对应 API Key 是否配置。
- 确认 Dify 服务可访问。
- 后端会对生活方案保留正式结构化兜底，避免患者页面崩溃；真实演示前仍建议确认 Dify 在线。

### 4. 图片破图或不显示

- 检查数据库中保存的图片路径是否存在。
- 检查 `backend/uploads/` 是否包含对应文件。
- 检查后端 `/uploads/**` 静态资源映射是否正常。

### 5. 数据为空

- 确认是否执行了测试数据脚本。
- 模块7 内容种子会在后端启动时幂等补齐文章和首页内容，但旧数据缺图时建议在管理端手动替换。

## 构建检查

前端构建：

```bash
cd frontend
npm run build
```

后端编译：

```bash
cd backend
mvn -DskipTests compile
```

## 演示流程建议

1. 启动 MySQL。
2. 启动后端，确认 `/api/health` 正常。
3. 启动前端。
4. 用患者账号登录，检查首页、个人中心、生活方案、健康资讯。
5. 用管理员账号登录，检查用户管理、生活方案记录、健康资讯管理、首页内容管理、轮播图管理。
6. 如果修改内容或图片，刷新患者端页面确认展示同步。
