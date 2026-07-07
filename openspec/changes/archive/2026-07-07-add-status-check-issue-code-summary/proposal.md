## Why

`dataspec-status-check` 在 active change 较多时会输出多条相同 `OPENSPEC_ACTIVE_CHANGE_PRESENT` warning。AI agent 需要遍历并聚合 `issues[]` 才能判断这是“一个问题类型重复多次”，容易把同类 warning 误判成多类风险。

## What Changes

- 在 `dataspec-status-check` JSON 的 `summary` 中兼容新增 `issueCodes[]`。
- 每个 `issueCodes[]` 项包含 `code`、`count` 和该 code 的最高 `severity`。
- 保持现有 `status`、退出码、`summary.errors/warnings`、`checks[]`、`issues[]` 和 `nextActions[]` 语义不变。
- 更新 AI 契约说明，并增加 Node 单测覆盖重复 warning code 和 error code 汇总。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `ai-contract-fixtures`: 状态检查输出增加 issue code 级摘要，方便 AI 消费本地状态结果。

## Impact

- 影响 `tools/dataspec-status-check.mjs`、`tools/dataspec-status-check.test.mjs` 和 `docs/ai-contracts.md`。
- 不新增命令、不改退出码、不访问网络或业务数据。
- 这是 AI 可读 JSON 的兼容性新增字段，按 SDD standard 记录并在 commit 前执行独立只读评审。
