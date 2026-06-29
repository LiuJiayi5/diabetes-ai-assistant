# 本地运行指南

本文档描述当前整合后的项目运行方式。旧的分支占位账号和零散 SQL 已废弃，数据库统一使用 `database/01_init_schema.sql` 与 `database/02_seed_demo_data.sql`。

## 1. 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8+
- 可选：Dify 服务

## 2. 初始化数据库

```bash
mysql --default-character-set=utf8mb4 -uroot -p < database/01_init_schema.sql
mysql --default-character-set=utf8mb4 -uroot -p diabetes_assistant < database/02_seed_demo_data.sql
```

第一条脚本会重建表结构，请先确认本地数据可以被清空。

## 3. 后端配置

开发环境默认配置在 `backend/src/main/resources/application-dev.yml`。常用环境变量如下：

```text
DB_URL=jdbc:mysql://localhost:3306/diabetes_assistant?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=123456
SMTP_HOST=your_smtp_host
SMTP_PORT=465
SMTP_PROTOCOL=smtps
SMTP_USERNAME=your_sender_email@example.com
SMTP_AUTH_CODE=your_smtp_auth_code
DIFY_BASE_URL=http://localhost/v1
DIFY_RISK_PREDICT_API_KEY=风险预测工作流 Key
DIFY_LIFE_PLAN_API_KEY=生活方案工作流 Key
DIFY_CHECKIN_ANALYSIS_API_KEY=打卡分析工作流 Key
DIFY_AI_DOCTOR_API_KEY=AI 医生 Agent Key
DIFY_INTERVENTION_REVIEW_API_KEY=周期复盘/自动干预工作流 Key
DIFY_COMPREHENSIVE_REPORT_API_KEY=综合健康报告工作流 Key
JWT_SECRET=diabetes_assistant_dev_secret
```

??????????? `SMTP_HOST`?`SMTP_PORT`?`SMTP_USERNAME`?`SMTP_AUTH_CODE`????? Dify ??AI ??????????????????????????

## 4. 启动后端

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

默认地址：`http://localhost:8080`。

## 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`。

## 6. 演示账号

```text
管理端：system_admin / 123456
患者端：li_ming / 123456
患者端：zhao_qing / 123456
患者端：he_yan / 123456
停用患者：zhou_bo / 123456
```

## 7. 验证命令

```bash
cd backend
mvn test
```

```bash
cd frontend
npm run build
```

