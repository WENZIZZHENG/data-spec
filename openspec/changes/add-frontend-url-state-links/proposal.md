## Why

DataSpec Web 已经覆盖 SQL 校验、字段库、覆盖率、反向导入和 AI 回放等高频工作流，但很多页面状态仍停留在内存或 localStorage 中，刷新、新标签页或交给 AI/browser automation 时会丢失上下文。P6-44 要把关键筛选和详情状态变成可复制、可恢复、非敏感的 URL 协议。

## What Changes

- 为关键前端页面定义统一的 URL query 状态协议，覆盖当前项目、字段库筛选、SQL 检查记录详情、AI 回放详情、覆盖率表筛选和反向导入批次详情。
- 新增轻量 URL 状态解析/写入工具，页面可从 query 初始化状态，并在用户操作时同步非敏感参数。
- 在关键页面提供复制当前操作链接入口，复制链接能在新标签页恢复同一项目、筛选条件和详情记录。
- 对无效、过期或不可访问的 URL 参数给出可恢复提示，并保持页面可继续使用。
- 明确安全边界：不把 SQL 原文、数据库连接信息、password、token、Authorization、完整 JDBC URL 或其他敏感输入写入 URL。

## Capabilities

### New Capabilities

- `frontend-url-state-links`: DataSpec Web 的 URL 状态协议、复制链接入口、无效参数兜底和敏感字段排除规则。

### Modified Capabilities

- 无。

## Impact

- 前端：新增 URL 状态 utility 和测试；改造 `FieldLibrary.vue`、`SqlLint.vue`、`AiReplay.vue`、`FieldCoverage.vue`、`ReverseImport.vue` 等关键页面的 query 初始化、状态同步和复制链接入口。
- 路由/项目状态：复用现有 Vue Router、Pinia project store 和统一页面状态，不重做整体路由体系。
- 文档/规范：更新 README、TODO 和 OpenSpec spec，说明 URL 状态支持范围与敏感信息边界。
- 后端/API：第一版不新增后端接口，不改变现有响应结构。

## Verification Evidence

- `openspec validate add-frontend-url-state-links --strict`：通过。
- `pnpm test`（`dataspec-web`）：通过，96 tests / 96 pass / 0 fail。
- `pnpm build`（`dataspec-web`）：通过；仍有既有 `@vueuse/core` pure annotation 与 chunk size warning。
- `git diff --check`：通过，仅有 Windows 行尾提示。
- 本地结构化代码评审：已执行，不使用子 agent；已修复 SQL 检查记录分页加载、AI 回放分页 URL 同步、字段详情链接重复打开状态和 query 同步保留 hash 四个 findings。
