## Why

`standard-evidence-review` 已经接入 workflow recipe、AI Context、MCP task card schema 和文档，但这些清单目前分散在代码、`docs/ai-contracts.md` 与 `TODO.md` 中。后续新增或删除 recipe 时，如果只更新其中一处，AI agent 会拿到不完整的任务路径，影响“稳定理解、生成、导入、校验和维护数据字段标准”的主目标。

## What Changes

- 扩展 `tools/dataspec-status-check.mjs`，把 workflow recipe id 清单纳入确定性状态检查。
- 以 `tools/dataspec-workflows.mjs` 的 `supportedWorkflowRecipeIds()` 作为 canonical source，检查 `docs/ai-contracts.md` 和 `TODO.md` 中声明的 recipe id 是否完整同步。
- 补充 Node 单测，先用缺失 `standard-evidence-review` 的 fixture 证明检查会失败，再验证同步后的 fixture 通过。
- 更新 README 对 `dataspec-status-check` 的说明，明确它会检查 AI workflow recipe 文档漂移。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `ai-workflow-recipes`: 增加本地状态检查对 workflow recipe 文档契约漂移的检测要求。

## Impact

- 影响 `tools/dataspec-status-check.mjs`、`tools/dataspec-status-check.test.mjs` 和 README 验证说明。
- 不新增后端 API、数据库表、远端服务调用或自动执行 workflow。
- 该变更属于 AI/workflow 可观察契约的质量门禁，按 SDD standard 处理；commit 前执行独立只读子 agent 评审。
