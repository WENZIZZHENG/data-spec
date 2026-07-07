## Why

`dataspec-status-check` 的 JSON 输出已经包含每个 check 的 `errorCount` / `warningCount`，但默认 text 输出只显示总问题数和问题明细。人类或只读取 text 输出的 agent 需要扫完整 issue 列表，才能知道哪个检查项处于 warning-only 或 error 状态。

## What Changes

- 在 status-check text 输出中增加“检查项”摘要段。
- 每个 check 摘要行展示 `id`、`status`、`issueCount`、`errorCount` 和 `warningCount`。
- 保持 JSON 输出、退出码、issue code 和现有问题明细不变。
- 增加 Node 单测锁定 text 输出能展示 check 级 severity 分布。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `ai-contract-fixtures`: 状态检查 text 输出增加 check 级 severity 摘要，便于本地维护和 AI 读取非 JSON 输出时定位风险。

## Impact

- 影响 `tools/dataspec-status-check.mjs` 和 `tools/dataspec-status-check.test.mjs`。
- 不新增命令、不改 JSON shape、不改退出码、不访问网络或业务数据。
- 这是 CLI text 输出的兼容增强，按 SDD standard 记录并在 commit 前执行独立只读评审。
