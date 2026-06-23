# 模块8：生活打卡与行为分析

负责功能：今日打卡、历史打卡、完成率统计、行为分析。
入口路由：/app/checkin、/admin/checkins、/admin/checkin-analysis。
后端接口：src/api/checkin.js。
是否涉及 Dify：普通打卡否；打卡分析由 Spring Boot 调用 checkin_behavior_analysis_workflow。
当前阶段：入口页、路由、API 骨架。
