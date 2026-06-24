# database 目录说明

本目录用于存放糖尿病预治智能助手的 MySQL 数据库脚本。

## 建表脚本

当前正式建库建表脚本为：

```text
database/01_init_schema.sql
```

该脚本包含：

- 数据库：`diabetes_assistant`
- 字符集：`utf8mb4`
- 排序规则：`utf8mb4_unicode_ci`
- 引擎：`InnoDB`
- 表数量：14 张

## 本地执行示例

在项目根目录执行：

```bash
mysql --default-character-set=utf8mb4 -u root -p < database/01_init_schema.sql
```

执行后可用以下 SQL 验证：

```sql
SHOW DATABASES;
USE diabetes_assistant;
SHOW TABLES;
DESCRIBE users;
DESCRIBE checkin_records;
DESCRIBE checkin_analysis;
```

普通业务数据由 Spring Boot 通过 MyBatis-Plus 读写 MySQL。Dify / DeepSeek 只负责 AI 能力生成，不作为普通业务后端。
