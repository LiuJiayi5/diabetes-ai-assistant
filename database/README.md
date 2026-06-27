# 数据库脚本说明

本目录只保留两份可执行 SQL，用于从零重建项目数据库并加载干净演示数据。

## 文件

```text
01_init_schema.sql      创建 `diabetes_assistant` 数据库、14 张业务表、索引和外键
02_seed_demo_data.sql   写入演示账号、健康档案、指标、风险评估、方案、打卡、AI 会话、健康资讯、轮播图和专家身份
```

`01_init_schema.sql` 会先删除再创建表，适合本地开发和演示环境重置。生产或重要数据环境不要直接执行。

## 执行顺序

在项目根目录执行：

```bash
mysql --default-character-set=utf8mb4 -uroot -p < database/01_init_schema.sql
mysql --default-character-set=utf8mb4 -uroot -p diabetes_assistant < database/02_seed_demo_data.sql
```

本地默认密码如果是 `123456`，也可以使用：

```bash
mysql --default-character-set=utf8mb4 -uroot -p123456 < database/01_init_schema.sql
mysql --default-character-set=utf8mb4 -uroot -p123456 diabetes_assistant < database/02_seed_demo_data.sql
```

## 演示账号

```text
system_admin / 123456   管理员
li_ming / 123456        患者，风险中等，数据完整
zhao_qing / 123456      患者，风险较低，记录稳定
he_yan / 123456         患者，风险偏高，适合查看复查和随访场景
zhou_bo / 123456        停用患者，用于管理端异常状态展示
```

健康资讯、首页轮播图和医生专家数据沿用当前项目中已经调好的展示数据；其他演示数据按患者健康管理流程重新整理，尽量保持关系真实、风格统一。

