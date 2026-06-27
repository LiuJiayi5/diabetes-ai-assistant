# 后端 SQL 资源目录

当前正式数据库脚本统一放在项目根目录的 `database/` 下：

```text
database/01_init_schema.sql
database/02_seed_demo_data.sql
```

`backend/src/main/resources/sql/` 暂不承载运行时自动执行脚本，避免 Spring Boot 启动时误清空本地数据库。需要重建数据库时请手动执行 `database/` 中的两份 SQL。

