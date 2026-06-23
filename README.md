# 糖尿病预治智能助手

## 项目简介

糖尿病预治智能助手是面向 patient 患者用户和 admin 管理员的 Web 管理平台。

系统不设置真实医生角色。“医生咨询”“名医在线交流”“AI 助手”统一理解为 AI 医生咨询，由 Dify Agent、糖尿病知识库和 DeepSeek 实现。

## 最终架构

```text
Vue 3 前端 + Spring Boot 主业务后端 + MySQL 数据库 + Dify / DeepSeek AI 能力
```

普通业务由 Spring Boot 实现，包括认证、数据库读写、管理端接口和常规业务接口。AI 功能由 Spring Boot 调用 Dify / DeepSeek 实现，前端不直接调用 Dify。

## 模块说明

- 模块1：用户认证与用户管理
- 模块2：健康档案
- 模块3：健康数据
- 模块4：糖尿病风险预测，调用 Dify
- 模块5：AI 医生咨询，调用 Dify Agent / 知识库
- 模块6：个性化生活方案，调用 Dify
- 模块7：内容与首页管理
- 模块8：生活打卡与行为分析，其中打卡分析调用 Dify

## 目录结构

```text
project-root/
  frontend/
  backend/
  database/
  dify/
  docs/
  README.md
```

## 前端启动方式

```bash
cd frontend
npm install
npm run dev
```

## 后端启动方式

```bash
cd backend
mvn spring-boot:run
```

打包：

```bash
cd backend
mvn clean package
```

## 数据库说明

当前阶段只保留数据库占位文件，不写正式建表 SQL。正式表结构将在模块契约和接口稳定后统一生成。

## Dify 说明

Dify 只用于模块4、模块5、模块6、模块8中的 AI 能力。普通业务接口、认证、数据库读写和管理端接口全部由 Spring Boot 承担。

## 后续开发顺序

1. 细化后端 API 契约和数据库表结构
2. 实现各模块 Entity、Mapper、Service、Controller
3. 实现各模块 contract 接口
4. 接入 JWT 鉴权与权限控制
5. 搭建 Dify Workflow / Agent / 知识库
6. 前端按模块扩展页面和业务流程
7. 补充测试、部署和生产配置
