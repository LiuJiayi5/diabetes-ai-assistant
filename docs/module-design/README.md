# 模块设计说明

当前项目已经从早期骨架阶段进入整合实现阶段。模块边界以根目录 `README.md`、后端代码、前端路由和 `docs/backend-module-contracts.md` 为准。

历史阶段中“暂不生成正式 SQL / 仅保留占位页面”的说明已经废弃。正式数据库脚本位于：

```text
database/01_init_schema.sql
database/02_seed_demo_data.sql
```

