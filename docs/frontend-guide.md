# 前端开发说明

前端位于 `frontend/`，使用 Vue 3 + Vite。患者端和管理端共用登录状态、API 封装和部分基础组件，但页面风格分别适配移动端和后台管理端。

## 目录

```text
frontend/src/
  api/                 后端接口封装
  components/          通用组件与移动端组件
  layouts/             PatientLayout、AdminLayout
  modules/             按业务模块组织页面、路由和局部样式
  router/              全局路由
  stores/              Pinia 状态
  styles/              全局变量、管理端公共页面样式
  utils/               token、图片地址、请求等工具
```

## 运行

```bash
cd frontend
npm install
npm run dev
```

## 构建

```bash
npm run build
```

## 约定

- 患者端页面使用 `/app/...` 路由，运行在手机壳容器内。
- 管理端页面使用 `/admin/...` 路由，保持 Element Plus 后台风格。
- API 返回值统一通过 `src/api/request.js` 和各模块 API 文件处理。
- AI 对话内容可使用 `components/MarkdownContent.vue` 渲染 Markdown，其他结构化结果优先由后端解析为字段展示。

