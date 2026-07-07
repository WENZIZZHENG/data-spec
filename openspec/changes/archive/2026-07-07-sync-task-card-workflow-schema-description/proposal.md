## Why

`standard-evidence-review` 已加入 workflow recipes，但 MCP `create_task_card.workflowId` 的 schema description 仍只列旧 recipe。MCP client 依赖 tool schema 做参数选择时，可能因此不知道能用 task card 创建标准证据复核任务。

## What Changes

- 将 `create_task_card.workflowId` 的 MCP schema description 改为从 `supportedWorkflowRecipeIds()` 生成，避免手写列表漂移。
- 补充 MCP `tools/list` 测试，确保 schema description 包含 `standard-evidence-review` 和全部已支持 workflow id。
- 不改变 `create_task_card` 入参、返回结构、校验逻辑或执行行为。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `dataspec-mcp`: `create_task_card` tool 的 `workflowId` schema description 必须覆盖当前支持的 workflow recipe id，避免 AI client 读到过期示例。

## Impact

- Tools：更新 `tools/dataspec-mcp.mjs` 和 `tools/dataspec-mcp.test.mjs`。
- OpenSpec：修改 `dataspec-mcp` 规格，记录 task card tool schema description 与 workflow catalog 同步的要求。
- 安全：只修改 schema description，不新增 token、password、Authorization、JDBC URL、DSN 或业务数据行。
- 验证：运行 MCP 定点测试、OpenSpec strict、`git diff --check`、敏感词扫描和独立子 agent 评审。
