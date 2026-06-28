## Why

AI agent 常见工作不是校验一条 SQL，而是批量扫描业务仓库、多个 SQL 文件或一批数据库表，再把问题、fixedSql、未纳管字段和下一步动作交付给用户。当前 `lint-files` 已能批量 lint，但缺少统一 batch run ID、分项结果、可下载 JSON 交付包和前端结果视图，AI 需要自己拼散落输出。

## What Changes

- 新增轻量 AI batch run 模型和 API，用于创建、查询和下载批量任务结果包。
- 第一版支持 SQL 文件/SQL 文本批量 lint 结果聚合；复用现有 `SqlLintService`、`fixedSql`、方言诊断和 SQL 检查记录能力。
- CLI `lint-files` 在保持原 JSON 输出兼容的基础上，可输出 batch delivery package 到文件，并包含 summary、files、issues、fixedSql、unmanaged hints、nextActions 和 evidence。
- 前端新增或扩展批量结果视图，能查看最近 batch run、文件级结果、错误/警告/建议统计和下载交付包。
- 输出继续脱敏，不包含 API token、数据库密码、完整连接串或业务数据行；不自动修改业务仓库文件。

## Capabilities

### New Capabilities

- `ai-batch-delivery-package`: 覆盖 AI 批量任务运行、结果聚合、机器可读交付包、CLI 输出和前端查看。

### Modified Capabilities

- 无。现有 `lint-files`、SQL lint、AI 回放和检查记录保持兼容；本变更以新增 batch delivery 契约承接批量场景。

## Impact

- 后端：新增 batch run DTO/service/controller，可复用 SQL lint 和检查记录；必要时新增 Flyway 表保存 batch 摘要和压缩 JSON payload。
- CLI：增强 `lint-files` 输出，可生成本地 JSON 交付包或透传后端 batch API。
- 前端：新增 API wrapper、类型和批量结果页或现有页面入口，展示最近任务与详情。
- 测试：新增后端 service/controller 单测、CLI JSON/文件输出测试、前端 utility/smoke 测试；验证接入 `mvn test`、`pnpm test`、`pnpm build`、`node --test` 和 OpenSpec validate。

## Verification Evidence

- `mvn test`（`dataspec-server`）：251 tests, 0 failures, 0 errors。
- `node --test tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs`：59 tests, 0 failures。
- `pnpm test`（`dataspec-web`）：63 tests, 0 failures。
- `pnpm build`（`dataspec-web`）：通过；仅有依赖 `@vueuse/core` pure annotation 和 chunk size 警告。
- `npx.cmd openspec validate add-ai-batch-delivery-package`：valid。
- `git diff --check`：通过；仅有 Windows CRLF 替换提示。
- 本地结构化代码评审：已修复 V15 时间列与 `LocalDateTime` 映射风格不一致、CLI 互斥参数晚校验导致潜在副作用两个 findings；未使用子 agent。
