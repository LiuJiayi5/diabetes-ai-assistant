# 数据库脚本说明

## 全新环境（推荐）

```text
01_init_schema.sql      建库建表（含推荐、复盘等最新表结构）
02_seed_demo_data.sql   演示数据
```

在项目根目录执行：

```bash
mysql --default-character-set=utf8mb4 -uroot -p < database/01_init_schema.sql
mysql --default-character-set=utf8mb4 -uroot -p diabetes_assistant < database/02_seed_demo_data.sql
```

`01_init_schema.sql` 会先删除再创建表，仅适合本地开发/演示重置。

## 已有旧库时的增量脚本（按需执行）

若数据库是较早版本建的，可**只执行你库里还没有对应表/字段的脚本**（均为增量、可重复思路见各文件注释）：

```text
03_intervention_review_migration.sql      生活方案来源字段 + intervention_reviews 表
03_add_article_recommendation_tables.sql  个性化推荐三表 + article_tags（队友新增）
04_patch_liming_risk_trend.sql            李明风险评估趋势演示数据（UPDATE）
05_patch_health_metric_12_utf8.sql        修复指标乱码
06_patch_risk_assessment_8.sql            修复评估 #8 详情字段
```

> 全新执行过 `01_init_schema.sql` 的库，一般**不需要**再跑 `03_add_article_recommendation_tables.sql`；`04`～`06` 仅在演示数据需要修补时执行。

## 演示账号

```text
system_admin / 123456   管理员
li_ming / 123456        患者，风险中等，数据完整
zhao_qing / 123456      患者，风险较低，记录稳定
he_yan / 123456         患者，风险偏高，适合查看复查和随访场景
zhou_bo / 123456        停用患者，用于管理端异常状态展示
```

健康资讯、首页轮播图和医生专家数据沿用当前项目中已经调好的展示数据；其他演示数据按患者健康管理流程重新整理，尽量保持关系真实、风格统一。

