# database 目录说明

本目录用于存放糖尿病预治智能助手的 MySQL 数据库脚本。

当前阶段只搭建项目骨架，不生成正式建表 SQL，不生成测试数据。

普通业务数据由 Spring Boot 通过 MyBatis-Plus 读写 MySQL。Dify / DeepSeek 只负责 AI 能力生成，不作为普通业务后端。
