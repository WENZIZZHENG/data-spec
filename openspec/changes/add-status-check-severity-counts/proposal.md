## Why

`dataspec-status-check` 的顶层 `status` 能区分 `warn`，但 `checks[]` 里的单项目前只有 `status` 和 `issueCount`。当某个 check 只有 warning 时，单项 `status` 仍为 `pass`，AI agent 需要再遍历 `issues[]` 才能判断风险分布，容易漏看 warning-only 的 OpenSpec active change 提醒。

## What Changes

- 为 `dataspec-status-check` 的每个 `checks[]` 项增加兼容字段 `errorCount` 和 `warningCount`。
- 保留现有 `status` 语义，不把 warning-only check 从 `pass` 改成 `warn`，避免破坏既有消费者。
- 补充 Node 单测，锁定 warning-only check 的 `warningCount` 与 error check 的 `errorCount`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `ai-contract-fixtures`: 状态检查输出增加兼容的 check 级 severity count，方便 AI 消费本地验证结果。

## Impact

- 影响 `tools/dataspec-status-check.mjs`、`tools/dataspec-status-check.test.mjs` 和 `docs/ai-contracts.md`。
- 不新增命令、不改退出码、不访问网络或业务数据。
- 这是 AI 可读 JSON 的兼容性新增字段，按 SDD standard 记录并在 commit 前执行独立只读评审。
