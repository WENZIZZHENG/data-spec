## Why

`dataspec-status-check` 已在 `summary.issueCodes[]` 提供问题编码计数，但 `nextActions[]` 仍只输出编码列表。AI agent 读取下一步建议时还需要额外回看 summary 才能判断问题规模和最高严重级，容易把重复 warning 误判为多类风险。

## What Changes

- 将 status-check 的“当前问题编码”下一步建议从纯 code 列表扩展为 `code(count=N,severity=level)` 摘要。
- 复用现有 `summary.issueCodes[]` 聚合结果，不新增第二套诊断或排序规则。
- 保持 `status`、退出码、`summary`、`checks[]`、`issues[]` 和前两条 `nextActions[]` 语义不变。
- 增加 Node 单测覆盖 warning-only 和 error 场景的 next action 摘要。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `ai-contract-fixtures`: 状态检查下一步建议增加 issue code 的数量和最高严重级摘要，方便 AI 消费本地状态结果。

## Impact

- 影响 `tools/dataspec-status-check.mjs`、`tools/dataspec-status-check.test.mjs` 和 `docs/ai-contracts.md`。
- 不新增命令、不改退出码、不访问网络或业务数据。
- 这是 AI 可读 `nextActions[]` 文本的兼容性增强，按 SDD standard 记录并在 commit 前执行独立只读评审。
