# 糖尿病预防智能助手

糖尿病预防智能助手是一个面向患者端和管理端的 Web 系统。患者端用于健康档案维护、健康数据记录、风险预测、生活方案、每日打卡、AI 医生咨询和健康资讯浏览；管理端用于用户、档案、健康数据、风险评估、生活方案、打卡、AI 咨询日志、专家身份、首页轮播和健康资讯管理。

AI 能力由后端统一接入 Dify Workflow / Agent。前端不直接调用 Dify，普通业务数据由 Spring Boot 写入 MySQL，方便管理端审计和双端同步。

## 功能概览

- 用户认证：患者注册、登录、记住登录状态、邮箱验证码、忘记密码重置、管理端登录。
- 健康档案：年龄、性别、身高、基础体重、腰围、家族史、慢病史、过敏史维护。
- 健康数据：体重、腰围、血压、空腹血糖、餐后血糖、糖化血红蛋白、饮食和运动状态记录。
- 风险预测：调用 Dify 风险预测工作流，保存结构化评估结果。
- 个性化方案：基于档案、指标和风险评估生成生活方案，并拆解为打卡任务。
- 打卡与分析：患者执行饮食、运动等任务，后端保存记录并支持行为分析。
- AI 医生：支持多个专家身份，每个会话绑定一个专家，Dify Agent 接收专家 persona 与用户上下文。
- 健康资讯：首页轮播、分类资讯、推荐文章及管理端内容维护。
- 管理端：用户、健康档案、健康数据、风险评估、方案、打卡、咨询和内容数据统一管理。

## 技术栈

- 前端：Vue 3、Vite、Pinia、Vue Router、Vant、Element Plus、lucide-vue-next、markdown-it。
- 后端：Spring Boot 3、MyBatis-Plus、Spring Security Crypto、JWT、Java Mail、MySQL 8。
- AI：Dify Workflow / Agent，可接入 DeepSeek 等模型。
- 数据库：MySQL，字符集建议 `utf8mb4`。

## 项目结构

```text
diabetes-ai-assistant/
  backend/                 Spring Boot 后端
  frontend/                Vue 3 前端
  database/                最终数据库建表与演示数据 SQL
  dify/                    Dify 工作流、Agent、知识库说明与导出文件
  docs/                    运行、架构、接口和团队协作文档
  README.md                项目总说明
```

## 环境准备

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8+
- 可选：本地或远端 Dify 服务

后端默认读取以下环境变量，未设置时会使用开发默认值：

```text
DB_URL=jdbc:mysql://localhost:3306/diabetes_assistant?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=123456
SMTP_QQ_USERNAME=2385448139@qq.com
SMTP_QQ_AUTH_CODE=你的 QQ 邮箱授权码
DIFY_BASE_URL=http://localhost/v1
DIFY_RISK_PREDICT_API_KEY=风险预测工作流 Key
DIFY_LIFE_PLAN_API_KEY=生活方案工作流 Key
DIFY_CHECKIN_ANALYSIS_API_KEY=打卡分析工作流 Key
DIFY_AI_DOCTOR_API_KEY=AI 医生 Agent Key
JWT_SECRET=diabetes_assistant_dev_secret
```

## 初始化数据库

数据库目录只保留两份正式 SQL：

```text
database/01_init_schema.sql      从零创建数据库、表结构、索引和外键
database/02_seed_demo_data.sql   初始化干净且完整的演示数据
```

在项目根目录执行：

```bash
mysql --default-character-set=utf8mb4 -uroot -p < database/01_init_schema.sql
mysql --default-character-set=utf8mb4 -uroot -p diabetes_assistant < database/02_seed_demo_data.sql
```

注意：`01_init_schema.sql` 会删除并重建相关表，执行前请确认本地数据可以被重置。

## 启动后端

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

默认后端地址：`http://localhost:8080`。

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认前端地址：`http://localhost:5173`。

## 演示账号

```text
管理端：system_admin / 123456
患者端：li_ming / 123456
患者端：zhao_qing / 123456
患者端：he_yan / 123456
```

`zhou_bo / 123456` 是停用患者账号，用于管理端停用和异常随访数据展示。

## 测试与构建

```bash
cd backend
mvn test
```

```bash
cd frontend
npm run build
```

## Dify 文件

- 风险预测：`dify/workflows/diabetes_risk_prediction_workflow.yml`
- 个性化生活方案：`dify/workflows/personalized_life_plan_workflow.yml`
- 打卡行为分析：`dify/workflows/checkin_behavior_analysis_workflow.md`
- AI 医生 Agent：`dify/agents/exports/diabetes_ai_doctor_agent.yml`
- 知识库材料：`dify/knowledge-base/docs/`

本地没有完整部署 Dify 时，后端测试会使用 mock 或跳过真实 AI 调用；真实运行 AI 功能需要先在 Dify 导入对应 DSL 并配置环境变量 Key。

