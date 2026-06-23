# 前端开发指南

## 前端目录说明

前端页面按业务模块放在 `src/modules/{module}/views/` 中。公共 API 封装放在 `src/api/`，模块内 `api.js` 只做重导出，方便模块负责人定位。

## 模块级入口页面说明

当前阶段只创建模块入口页，用于验证路由、明确模块边界和后续开发位置，不提前细拆所有功能页。

## 如何新增模块子页面

1. 在对应模块 `views/` 下新增页面。
2. 在对应模块 `routes.js` 中新增路由。
3. 如需接口，先在 `src/api/*.js` 中封装。
4. 页面只调用模块 API，不直接写 axios。

## 如何调用 src/api/*.js

```js
import { getMyProfile } from '@/api/profile'

const result = await getMyProfile()
```

## 如何保存 token

`src/utils/token.js` 提供：

- getToken
- setToken
- removeToken

`src/api/request.js` 会自动携带：

```text
Authorization: Bearer token
```

## 如何接入后端接口

在 `.env.development` 中配置：

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

## 患者端和管理端布局说明

- PatientLayout：患者端 `/app/*`
- AdminLayout：管理端 `/admin/*`
- BlankLayout：登录、注册等独立页面
