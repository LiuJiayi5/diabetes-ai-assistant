# Figma 管理端视觉一比一复刻对照文档

## 1. 代码来源索引

来源：Figma Make `管理员后台原型设计`

| 页面 | Figma 文件 | 路由入口 | 主要组件 |
| -- | -- | -- | -- |
| 管理员登录 | `src/app/pages/LoginPage.tsx` | `/login` | `LoginPage` |
| 后台布局 | `src/app/components/layout/DashboardLayout.tsx`、`Sidebar.tsx`、`TopBar.tsx` | 后台所有页面 | `DashboardLayout`、`Sidebar`、`TopBar` |
| 首页概览 | `src/app/pages/DashboardPage.tsx` | `/dashboard` | `DashboardPage` |
| 用户管理 | `src/app/pages/UsersPage.tsx` | `/users` | `UsersPage` |
| 用户详情 | `src/app/pages/UserDetailPage.tsx` | `/users/:id` | `UserDetailPage` |
| 生活方案记录 | `src/app/pages/LifePlansPage.tsx` | `/life-plans` | `LifePlansPage` |
| 生活方案详情 | `src/app/pages/LifePlanDetailPage.tsx` | `/life-plans/:id` | `LifePlanDetailPage` |
| 生成日志 | `src/app/pages/LifePlanLogPage.tsx` | `/life-plans/:id/log` | `LifePlanLogPage` |
| 健康资讯管理 | `src/app/pages/ArticlesPage.tsx` | `/articles` | `ArticlesPage` |
| 资讯编辑 | `src/app/pages/ArticleEditPage.tsx` | `/articles/new`、`/articles/:id/edit` | `ArticleEditPage` |
| 首页内容管理 | `src/app/pages/HomeContentPage.tsx` | `/home-content` | `HomeContentPage` |
| 专家展示管理 | `src/app/pages/ExpertsPage.tsx` | `/experts` | `ExpertsPage` |

## 2. 全局视觉 Token

```css
:root {
  --admin-font-family: "Inter", "Noto Sans SC", system-ui, sans-serif;
  --admin-font-size-root: 15px;
  --admin-bg: #F5F8FF;
  --admin-login-bg: linear-gradient(135deg, #EBF0FF 0%, #E0EAFF 50%, #D6E4FF 100%);
  --admin-login-panel: linear-gradient(155deg, #0B1B3A 0%, #0F2554 55%, #183B72 100%);
  --admin-sidebar-bg: #0D1B36;
  --admin-sidebar-active: linear-gradient(135deg,#5C8EF8 0%,#4A7BF5 100%);
  --admin-primary: #5C8EF8;
  --admin-primary-dark: #4A7BF5;
  --admin-primary-soft: #EEF3FF;
  --admin-primary-gradient: linear-gradient(135deg,#5C8EF8,#4A7BF5);
  --admin-content-gradient: linear-gradient(135deg,#2563EB,#38BDF8);
  --admin-card-bg: #FFFFFF;
  --admin-card-muted: #F8FAFF;
  --admin-input-bg: #F8FAFF;
  --admin-input-border: #DCE6F8;
  --admin-text-strong: #1A2E4A;
  --admin-text-title: #172554;
  --admin-text-secondary: #64748B;
  --admin-text-muted: #94A3B8;
  --admin-border: rgba(92,142,248,0.10);
  --admin-border-solid: #E5EAF3;
  --admin-success: #22C55E;
  --admin-success-text: #15803D;
  --admin-danger: #EF4444;
  --admin-danger-text: #DC2626;
  --admin-warning: #F59E0B;
  --admin-warning-text: #B45309;
  --admin-radius-card: 12px;
  --admin-radius-dialog: 16px;
  --admin-radius-input: 12px;
  --admin-shadow-card: 0 1px 8px rgba(92,142,248,0.07);
  --admin-shadow-blue: 0 3px 10px rgba(92,142,248,0.30);
  --admin-shadow-login: 0 20px 60px rgba(92,142,248,0.15), 0 4px 16px rgba(0,0,0,0.06);
  --admin-sidebar-width: 256px;
  --admin-topbar-height: 56px;
}
```

## 3. 字体规范

| 区域 | 字体族 | 字号 | 字重 | 颜色 | 行高 |
| -- | -- | -- | -- | -- | -- |
| 页面标题 | `Inter`, `Noto Sans SC`, system-ui | 24px | 700 | `#1A2E4A` / `#172554` | 1.5 |
| 页面说明 | 同上 | 14px | 400 | `#6B82A4` / `#64748B` | 1.5 |
| 卡片标题 | 同上 | 16px | 600 | `#1A2E4A` / `#172554` | 1.5 |
| 表格表头 | 同上 | 12px | 600 | `#94A3B8` | uppercase / tracking |
| 表格正文 | 同上 | 13px-14px | 400 / 500 | `#64748B` / `#172554` | 1.5 |
| 表单标签 | 同上 | 12px-14px | 500 / 600 | `#172554` / `#94A3B8` | 1.5 |
| 按钮文字 | 同上 | 13px-14px | 500 / 600 | `#FFFFFF` 或 `#64748B` | 1.5 |
| 侧边栏文字 | 同上 | 14px | 500 | 未选中 `#A8C8EE`，选中 `#FFFFFF` | 1.5 |

## 4. 图标来源清单

Figma 使用 `lucide-react`，Vue 复刻时使用 `lucide-vue-next` 对应组件。

| 图标 | 出现位置 | 含义 | 尺寸 | 颜色 / 背景 |
| -- | -- | -- | -- | -- |
| `Leaf` | 登录左侧、侧边栏 Logo | 系统品牌 | 20px | 白色，容器 `40/44px`，渐变 `#5C8EF8 -> #7EB5FF` |
| `Shield` | Logo 角标、登录页特性 | 管理端/安全 | 12px-20px | 白色或 `#5C8EF8` |
| `LayoutDashboard` | 侧边栏 | 首页概览 | 18px | 未选中 `#A8C8EE`，选中白色 |
| `Users` | 侧边栏、用户统计 | 用户管理 | 18px | `#5C8EF8` 或白色 |
| `FileText` | 生活方案 | 方案记录 | 18px | `#2563EB` 或白色 |
| `Newspaper` | 资讯管理 | 健康资讯 | 18px | 白色 / `#2563EB` |
| `Home` | 首页内容 | 首页配置 | 18px | 白色 / `#2563EB` |
| `Image` | 轮播图 | 图片内容 | 18px | `#CBD5E1` / `#2563EB` |
| `UserCheck` | 专家展示 | 展示卡片 | 18px | `#2563EB` |
| `Search` | 顶部栏、筛选区 | 搜索 | 14px-16px | `#94A3B8` |
| `Bell` | 顶部栏 | 通知 | 16px | `#6B82A4` |
| `LogOut` | 侧边栏底部 | 退出登录 | 18px | `#8BAABF`，hover `#F87171` |
| `CheckCircle` / `XCircle` | 状态标签、统计 | 成功/失败 | 11px-18px | 成功 `#22C55E`，失败 `#EF4444` |
| `Loader2` | 生成中 | loading | 11px-20px | `#2563EB` |
| `Eye` / `Edit2` / `Trash2` | 表格操作 | 预览/编辑/删除 | 14px | `#38BDF8` / `#2563EB` / `#EF4444` |
| `Plus` | 新增按钮 | 新增内容 | 16px | 白色 |
| `Star` | 推荐 | 首页推荐 | 14px-18px | `#F59E0B` |

## 5. 页面复刻表

| 页面 | 区域 | 组件 / 元素 | 字体 | 颜色 | 图标来源 | 尺寸 | 圆角 | 阴影 | 备注 |
| -- | -- | -- | -- | -- | -- | -- | -- | -- | -- |
| 管理员登录 | 外层背景 | 页面容器 | 15px | `linear-gradient(135deg,#EBF0FF,#E0EAFF,#D6E4FF)` | 无 | `min-height:100vh` | 0 | 无 | 背景另有 radial 光晕 |
| 管理员登录 | 登录卡片 | 白色主卡 | 标题 24px/700 | `#FFFFFF`、`#1A2E4A` | `Leaf`、`Shield`、`User`、`Lock` | `max-width:896px`，高约 `580px` | 16px | `0 20px 60px rgba(92,142,248,0.15)` | 左深蓝，右表单 |
| 后台布局 | 侧边栏 | 菜单 | 14px/500 | 背景 `#0D1B36`，文字 `#A8C8EE` | lucide | 宽 `256px` | 菜单 12px | 选中 `0 4px 14px rgba(92,142,248,0.35)` | 分组标题 `#4A7AAA` |
| 后台布局 | 顶部栏 | 面包屑、搜索、管理员 | 14px | 背景 `#FFFFFF`，边框 `rgba(92,142,248,0.10)` | `Search`、`Bell` | 高 `56px` | 搜索 8px | `0 1px 4px rgba(92,142,248,0.06)` | 右侧头像 28px |
| 用户管理 | 筛选区 | 输入框/选择器/按钮 | 12px-14px | 输入背景 `#F8FAFF`，边框 `#DCE6F8` | `Search` | 输入高 36px | 12px | `--admin-shadow-card` | 查询按钮蓝色渐变 |
| 用户管理 | 表格 | 用户列表 | 12px-14px | 表头 `#94A3B8`，正文 `#64748B` | 无 | 行高约 52px | 卡片 12px | `--admin-shadow-card` | 状态使用胶囊 |
| 生活方案 | 统计卡 | 总数/成功/失败 | 数字 24px/700 | `#172554` | `FileText` 等 | 图标容器 36px | 12px | `--admin-shadow-card` | 背景白色 |
| 生活方案 | 表格 | 方案记录 | 12px-14px | 风险：低绿/中黄/高红 | `Loader2`、`CheckCircle`、`XCircle` | 操作按钮高 28px | 8px | `--admin-shadow-card` | 管理端只查看，不生成 |
| 健康资讯 | 表格 | 资讯列表 | 12px-14px | 分类 `rgba(56,189,248,0.10)` / `#0369A1` | `Eye`、`Edit2`、`Trash2`、`Star` | 封面 `56x36px` | 8px / 12px | `--admin-shadow-card` | 不做 Dify 知识库 |
| 首页内容 | 推荐列表 | 首页推荐资讯 | 12px-14px | `#F8FAFF`，边框 `#E5EAF3` | `GripVertical`、`X` | 图片 `64x40px` | 12px | `--admin-shadow-card` | 简化排序 |

## 6. Vue 3 复刻注意事项

- Figma 源码为 React + Tailwind，只能作为视觉和交互参考；项目实际实现必须使用 Vue 3、Vue Router、Pinia、Axios、Element Plus。
- 管理端为桌面后台布局，不使用用户端 `MobileShell` 和底部 TabBar。
- 管理端颜色主轴是蓝色后台风格，用户端仍保留浅绿浅蓝移动端风格。
- 模块6管理端只查看 `life_plans` 记录、`input_summary`、`plan_json`、`checkin_tasks_json`、`error_message`，不在前端重新生成方案，不调用 Dify。
- 模块7管理端只管理用户可见资讯和首页内容，不做 Dify 知识库上传、切片、向量化、召回配置。
- 所有管理端接口通过 Spring Boot `/api/admin/**` 调用，前端不保存或暴露 Dify API Key。
