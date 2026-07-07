## Why

`dataspec-status-check` 在只有 warning 时仍提示“优先修复 severity=error”，会让 AI agent 误判当前仓库存在阻塞错误。刚新增的 check 级 severity count 已能暴露 warning-only 状态，行动建议也应与最高严重级保持一致。

## What Changes

- 让 `dataspec-status-check` 的 `nextActions[]` 按当前最高严重级生成首条建议。
- 当状态为 `warn` 且无 error 时，首条建议引导处理 warning 或确认可保留的 active change，而不是提示修复 error。
- 保持 `status`、退出码、`summary`、`checks[]` 和 `issues[]` JSON shape 不变。
- 增加 Node 单测，锁定 warning-only 输出不再包含误导性 error-first 文案。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `ai-contract-fixtures`: 状态检查的 AI 行动建议与当前最高 severity 对齐，避免 warning-only 结果被描述成 error 修复路径。

## Impact

- 影响 `tools/dataspec-status-check.mjs` 和 `tools/dataspec-status-check.test.mjs`。
- 可能更新 `docs/ai-contracts.md` 中 status-check 行动建议说明，若实现仅改变建议文本则不新增稳定字段。
- 不新增命令、不改退出码、不访问网络或业务数据。
- 这是 AI 可读 CLI 输出语义的兼容修正，按 SDD standard 记录并在 commit 前执行独立只读评审。
