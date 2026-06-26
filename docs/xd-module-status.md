# xd 模块实现现状

本文档记录 xd 分支中本人负责的模块现状，覆盖模块1 用户认证与基础信息、模块6 个性化生活方案、模块7 健康资讯与科普内容。内容基于当前代码与数据库脚本整理，便于演示和交接。

## 模块1：用户认证与基础信息

### 已完成功能

- 患者端支持注册、登录、退出、个人中心展示和账号资料编辑。
- 管理端支持独立登录入口、管理员个人中心、顶部头像和用户名展示。
- 患者和管理员共用 `users` 表，通过 `role` 区分 `patient` 与 `admin`。
- 头像、文章封面、首页内容图等图片上传统一走后端上传接口，不再要求用户填写图片 URL。
- 头像上传支持点击/拖拽选择、预览、删除、替换，并在患者端和管理员端支持拖动定位与缩放裁剪。
- 默认头像已调整为统一风格，避免未设置头像时出现半成品观感。
- 前端登录态按角色隔离：患者 token 使用 `diabetes_patient_jwt`，管理员 token 使用 `diabetes_admin_jwt`。
- 管理端接口增加后端统一角色过滤：`/api/admin/**` 必须使用 `role=admin` 的 JWT，患者 token 访问返回 403。
- 已修复患者端和管理端账号闪变、跨角色缓存污染、CORS 预检被误拦导致 `Network Error` 的问题。

### 前后端联动

- 前端认证 API：`frontend/src/api/auth.js`
- 前端登录态：`frontend/src/stores/auth.js`、`frontend/src/utils/token.js`
- 前端路由守卫：`frontend/src/router/index.js`
- 患者页面：`frontend/src/modules/auth/views/LoginView.vue`、`RegisterView.vue`、`AccountCenterView.vue`、`EditAccountView.vue`
- 管理端页面：`frontend/src/modules/admin/views/AdminLoginView.vue`、`AdminProfileView.vue`、`frontend/src/layouts/AdminLayout.vue`
- 后端认证控制器：`backend/src/main/java/com/diabetes/assistant/modules/user/controller/AuthController.java`
- 后端用户控制器：`UserController.java`、`AdminUserController.java`
- 后端用户服务：`UserServiceImpl.java`
- 管理端角色过滤器：`backend/src/main/java/com/diabetes/assistant/common/security/AdminApiRoleFilter.java`
- 上传接口：`backend/src/main/java/com/diabetes/assistant/common/controller/FileUploadController.java`

### 数据库和接口

- 主要表：`users`
- 关键字段：`username`、`password_hash`、`phone`、`email`、`avatar`、`role`、`status`、`last_login_time`
- 患者/管理员基础资料均通过 `/api/user/me` 读取和保存，但前端请求会显式指定 patient/admin 作用域。
- 管理员查看和维护用户状态使用 `/api/admin/users`、`/api/admin/users/{userId}/status`。

### 注意点

- 管理端演示账号以当前数据库为准，当前本地常用账号为 `module6_admin / 123456`。
- 患者端常用账号为 `module6_patient / 123456`。
- 如果浏览器保留了旧 localStorage，建议退出后重新登录；当前代码已能自动清理角色不匹配的会话。
- 上传文件保存在后端 `uploads/` 目录，该目录被 `.gitignore` 忽略，部署或换机时需要保留/迁移实际上传文件。

## 模块6：个性化生活方案

### 已完成功能

- 患者端生活方案页支持读取当前方案、历史方案、重新生成方案、调整生成偏好。
- 方案页恢复为正式移动端多卡片结构，包含饮食管理、运动管理、作息/控糖提醒等区块。
- 饮食管理展示早餐、午餐、晚餐、加餐等卡片；运动管理展示轻运动、有氧、抗阻/拉伸、注意事项等卡片。
- 一周安排不再一次铺满页面，改为 1 到 7 的数字按钮切换，每次只展示选中日期。
- 点击建议卡片可查看对应详情。
- 重新生成方案会走后端/Dify 调用链，并刷新当前方案内容。
- Dify 未配置或短时异常时，后端有结构化正式兜底方案，避免页面崩溃或展示乱码。
- 管理端支持查看生活方案记录、详情和调用日志，不在管理端重新生成方案。
- 已清理生活方案页面中的乱码、开发占位和过早换行问题。

### 前后端联动

- 患者端入口：`frontend/src/modules/lifeplan/views/LifePlanEntryView.vue`
- 历史/详情页：`LifePlanHistoryView.vue`、`LifePlanDetailView.vue`
- 方案弹窗：`frontend/src/modules/lifeplan/components/PlanGenerateDialog.vue`
- 前端 store/API：`frontend/src/stores/lifePlan.js`、`frontend/src/api/lifePlan.js`
- 方案解析兼容：`frontend/src/modules/lifeplan/utils.js`
- 后端控制器：`backend/src/main/java/com/diabetes/assistant/modules/lifeplan/controller/LifePlanController.java`
- 后端服务：`LifePlanServiceImpl.java`
- Dify 调用：`backend/src/main/java/com/diabetes/assistant/modules/dify/*`

### 数据库和接口

- 主要表：`life_plans`
- 关联表：`patient_profiles`、`health_metrics`、`risk_assessments`
- 关键字段：`plan_json`、`checkin_tasks_json`、`input_summary`、`call_status`、`error_message`、`status`
- 患者端接口包括当前方案、历史方案、详情、生成方案等。
- 管理端接口：`/api/admin/life-plans`
- Dify 配置来自环境变量或 `application-dev.yml`：
  - `DIFY_BASE_URL`
  - `DIFY_LIFE_PLAN_API_KEY`
  - `DIFY_RISK_PREDICT_API_KEY`

### 注意点

- 生成生活方案前，患者需要尽量具备健康档案、健康指标和风险评估数据。
- 演示真实 Dify 生成时，应确认本机 Dify 服务地址和 API Key 已配置，当前本地配置曾验证过生活方案和风险预测 key 可用。
- 当前后端保留正式兜底方案用于演示稳定性，但真实演示应优先确认 Dify 服务在线。
- 方案页面显示基于结构化 `dailySchedule`，同时兼容部分旧格式字符串和拆分字段。

## 模块7：健康资讯与科普内容

### 已完成功能

- 健康资讯内容已扩展到 6 个分类，每类至少 5 篇，总量至少 30 篇。
- 分类包括：饮食指导、运动指南、日常习惯、糖尿病科普、并发症预防、控糖误区。
- 每篇资讯包含标题、分类、摘要、正文、封面图、阅读量/展示数据、推荐/排序、创建/更新时间。
- 文章内容面向普通患者，避免绝对化医疗表达。
- 内容种子初始化在后端执行，具备幂等补齐逻辑，避免每次启动重复插入。
- 配图使用本地正式风格 SVG/资源兜底，避免破图、空白灰块和外链失效。
- 患者端健康资讯首页支持轮播、精选栏目、推荐文章、约 10 篇样例资讯。
- “更多”进入全部资讯列表，全部资讯页按 10 篇分页展示。
- 分类卡片进入独立分类资讯页，每个分类展示对应文章。
- 搜索支持标题、摘要、分类、正文、标签/关键词等字段的模糊匹配和权重排序。
- 管理端支持健康资讯、新增/编辑/删除/上下架、封面上传。
- 管理端支持首页内容、轮播图、专家展示等内容维护，图片上传不再手填 URL。

### 前后端联动

- 患者端首页和资讯：`frontend/src/modules/article/views/HomeEntryView.vue`、`ArticleEntryView.vue`、`ArticleListView.vue`、`ArticleCategoryView.vue`、`ArticleDetailView.vue`
- 前端资讯 store：`frontend/src/stores/articles.js`
- 前端 API：`frontend/src/api/article.js`
- 管理端页面：`AdminArticlesView.vue`、`AdminArticleEditView.vue`、`AdminHomeContentView.vue`、`AdminBannersView.vue`、`AdminExpertsView.vue`
- 管理端 API：`frontend/src/api/admin.js`
- 后端控制器：`backend/src/main/java/com/diabetes/assistant/modules/content/controller/ContentController.java`
- 后端服务：`ContentServiceImpl.java`
- 种子数据：`ContentSeedInitializer.java`

### 数据库和接口

- 主要表：`articles`、`home_contents`
- 文章分类枚举：`diet`、`exercise`、`habit`、`science`、`complication`、`mistake`
- 文章状态：`draft`、`published`、`offline`
- 首页内容类型：`banner`、`ai_doctor_card`
- 患者端接口包括文章列表、详情、分类、首页内容。
- 管理端接口包括：
  - `/api/admin/content-management`
  - `/api/admin/articles/save`
  - `/api/admin/articles/{articleId}`
  - `/api/admin/home-contents/save`
  - `/api/admin/home-contents/{contentId}`

### 注意点

- 后端种子图以本地 SVG/静态资源为主，如果后续追求更真实照片，可在管理端逐篇替换。
- 旧数据如图片路径失效，前端有兜底展示，但正式演示建议优先检查封面、轮播图和首页内容图。
- 管理端接口需要管理员 token；患者 token 会被后端统一拒绝。
- 管理端新增/编辑文章或轮播图保存后，患者端刷新即可看到最新数据。

## 当前整体演示建议

- 先启动 MySQL、后端、前端，再登录患者端和管理端。
- 患者端建议演示：登录、个人中心头像裁剪上传、生活方案 1-7 天切换、健康资讯首页/分类/详情。
- 管理端建议演示：管理员登录、个人中心、首页概览、用户管理、生活方案记录、健康资讯管理、首页内容/轮播图管理。
- 如果遇到 `Network Error`，优先检查后端 8080 是否启动、CORS 预检是否正常、当前账号是否为管理员。
