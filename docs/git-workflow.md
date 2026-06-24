# 团队 Git 分支规范

## 分支职责

- `main`
  - 最终稳定分支
  - 只保留阶段性稳定成果
- `develop`
  - 团队日常集成分支
  - 用于同步会影响其他成员开发的公共改动
- 个人开发分支
  - 每位成员使用自己的独立开发分支
  - 分支名可使用姓名首字母，例如 `xd`
  - 模块内部开发优先提交到个人分支

## 日常开发规则

1. 开发前先确认当前所在分支，默认在自己的个人分支上工作。
2. 模块内部页面、样式、mock 数据、个人负责模块的普通功能，先提交到个人分支。
3. 不要频繁把未成熟模块代码直接合并到 `develop`。
4. 合并到 `develop` 前，先同步最新远程分支并处理冲突。

## 哪些改动需要尽早同步到 develop

以下属于公共改动，建议尽早同步到 `develop`：

- 新增或修改项目依赖
- 修改公共组件
- 修改全局路由
- 修改公共 API 封装
- 修改 DTO / contract
- 修改数据库 SQL
- 修改公共配置
- 修改团队规范文档

同步时建议：

1. 先把公共改动单独整理成清晰 commit
2. 再把这部分 commit 合并或 cherry-pick 到 `develop`
3. 不要把未成熟模块页面一并带入 `develop`

## 提交与合并建议

- commit message 尽量清晰，例如：
  - `feat(frontend): implement module1 auth profile pages`
  - `chore(docs): add git workflow rules`
  - `chore(frontend): add shared icon dependency`
- 合并前先 `pull` 对应目标分支最新代码
- 合并公共内容后及时通知组员 `pull develop`

## 提交忽略规则

不要提交以下无关内容：

- `node_modules/`
- `dist/`
- `.env`
- IDE 缓存
- 临时日志
- 本地测试截图或临时导出文件

## 推荐流程

1. 从 `main` 拉出或更新 `develop`
2. 从 `develop` 拉出个人分支，例如 `xd`
3. 在个人分支完成模块开发
4. 公共改动先同步到 `develop`
5. 模块成熟后再从个人分支合并到 `develop`
6. 阶段稳定后再从 `develop` 合并到 `main`
