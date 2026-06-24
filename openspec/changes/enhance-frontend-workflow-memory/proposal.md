## Why

DataSpec 的前端核心流程已经可用，但反向导入和字段库之间还会丢失用户上下文：数据库连接信息、表筛选、差异筛选和导入后字段定位都需要重复操作。P5-8 的第一版目标是用最小前端改动减少重复输入，让个人日常使用更顺。

## What Changes

- 反向导入数据库直连页记住当前项目的非敏感连接信息、表选择、表搜索词和差异筛选条件。
- 记忆内容只保存在浏览器本地，并明确排除数据库密码、token、完整 JDBC URL 等敏感信息。
- 字段库支持从路由 query 接收关键词，导入完成后点击“查看字段库”自动携带最近导入字段名并筛选字段库。
- 增加前端纯函数测试，覆盖敏感字段剔除、项目隔离、损坏缓存兜底和字段库跳转 query 生成。

## Capabilities

### New Capabilities

- `frontend-workflow-memory`: 前端高频流程状态记忆、非敏感本地保存和跨页筛选联动。

### Modified Capabilities

无。

## Impact

- 影响前端：`dataspec-web/src/views/ReverseImport.vue`、`dataspec-web/src/views/FieldLibrary.vue`、新增前端 utility 和测试。
- 不影响后端 API、数据库模型、OpenAPI schema 或 CLI/MCP。
- 验证入口：`pnpm test`、`pnpm build`、`npx openspec validate enhance-frontend-workflow-memory`、`git diff --check`。
