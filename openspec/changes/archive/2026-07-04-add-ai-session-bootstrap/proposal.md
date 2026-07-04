## Why

AI agent 每次进入业务仓库时都要重新判断 DataSpec 服务、项目、token、标准版本、可用能力和下一步命令；这些信息分散在 doctor、AI Context、README 和 MCP 中，容易漏读或先调用错误入口。

## What Changes

- 新增只读 AI session bootstrap 包，聚合当前项目、服务地址、auth 模式、标准快照、能力清单、推荐命令、风险提示、文档引用和结构化 nextActions。
- 新增后端 API，用统一 JSON 契约返回启动包；缺 projectId、服务未授权、标准未版本化等场景返回可机器判断的 checks/nextActions。
- CLI 新增 `dataspec bootstrap`，优先读取后端启动包；服务不可达时输出本地 fallback bootstrap，而不是只报错退出。
- MCP 新增 session bootstrap resource/tool，AI 在 MCP 会话开头可以一跳读取当前上下文。
- 能力清单新增 `session-bootstrap` 能力说明，指向 API/CLI/MCP 入口。

## Capabilities

### New Capabilities
- `ai-session-bootstrap`: 面向 AI 新会话的只读启动握手包，说明当前能否 lint、导出 Context、反向导入或生成 DDL。

### Modified Capabilities
- `dataspec-cli`: CLI 需要提供 `bootstrap` 命令并保留结构化 fallback。
- `dataspec-mcp`: MCP 需要暴露 session bootstrap resource/tool。
- `ai-capability-catalog`: 能力清单需要包含 session bootstrap 入口。

## Impact

- 后端：新增 bootstrap DTO、service、controller；复用 capability catalog、standard snapshot、项目访问校验。
- CLI/MCP：新增命令、resource/tool 和对应 Node tests。
- OpenAPI/前端契约：新增 API 后重新生成 `dataspec-web/src/api/schema.ts`。
- 文档/TODO：README 和 TODO 更新当前能力与状态。

## Verification Evidence

- `mvn test`（`dataspec-server`）：416 tests, 0 failures, 0 errors, 0 skipped；保留既有 Maven transitive POM warning、Byte Buddy dynamic agent warning、SQL parser 非法 SQL 用例 warning。
- `pnpm test`（`dataspec-web`）：102 tests, 0 failures。
- `pnpm build`（`dataspec-web`）：`vue-tsc --noEmit && vite build` 通过；保留既有 Rolldown pure annotation 与 chunk size warning。
- `pnpm check:api`（`dataspec-web`）：`OpenAPI schema.ts 已是最新: src/api/schema.ts`。
- `node --test tools/*.test.mjs`：145 tests, 0 failures。
- `openspec validate --all`：95 items passed, 0 failed。
- `git diff --check`：exit 0，仅输出既有 CRLF line-ending warnings。

## Review Evidence

- 独立 agent `019f2c0e-d69f-7583-8652-5ef23fc7ee5a` 完成只读评审；结论为阻塞归档，发现 2 个 Important 和 1 个 Minor。
- Important 1：CLI 将 401/403 等后端拒绝误判为服务不可达 fallback。已修复为 401/403 返回 `BLOCKED` bootstrap JSON，并补 `bootstrap returns auth next action when server rejects token` 回归测试。
- Important 2：CLI fallback 命令使用 raw `server` 可能泄漏 URL userinfo。已改用 `safeServerForMetadata(server)` 并补 `bootstrap fallback redacts server userinfo in commands` 回归测试。
- Minor：MCP 无默认项目时无法读取启动包。已允许 `get_session_bootstrap` 无 projectId 调用后端，并补 `get_session_bootstrap tool works without configured project` 回归测试。
- 尝试启动二次复审 agent 时命中当前线程 agent 数量上限；本轮已满足“独立 agent 评审 + 修复 findings + 本地验证”的归档门槛。
