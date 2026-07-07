## Why

AI Context 已经支持裁剪、导出和预算预检，但导出成功不等于适合当前 AI 任务。用户和 AI Agent 还需要一个确定性的本地判断：这份上下文是否缺关键资源、是否被截断、token 是否被低价值内容占满，以及下一步应该继续使用、补导出还是停止。

## What Changes

- 新增 CLI 命令 `context-quality check`，第一版只做本地只读评分，可读取已导出的 AI Context 目录、AI Context zip，或 `context-budget plan` JSON。
- 输出稳定 JSON，包含 `contextQualityScore`、`qualityLevel`、`tokenBudgetBreakdown`、`missingCriticalResources`、`truncatedResources`、`coverageByCategory`、`taskFitHints` 和 `nextContextActions` 等 AI 可读字段。
- 支持 `--format text|json`，text 只作为人工摘要，JSON 作为稳定机器契约。
- 将命令加入 CLI/MCP contract fixture，防止后续删除稳定字段、退出码、安全说明或示例。
- 不新增后端 API，不调用外部 LLM，不写 `.dataspec/context/` 缓存，也不修改项目状态。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `dataspec-cli`：新增 `context-quality check` CLI 契约，用于本地只读评估 AI Context 可用性和任务适配度。

## Impact

- 影响 `tools/dataspec-cli.mjs`、CLI 单测、CLI/MCP contract fixture 和 `dataspec-cli` OpenSpec delta。
- 只新增本地读文件和解析逻辑；不改变现有 API route、数据库 schema、持久化语义或后端行为。
- 因新增 CLI/AI 可观察协议，按 SDD standard 执行，并在交付/commit 前进行独立代码评审。
