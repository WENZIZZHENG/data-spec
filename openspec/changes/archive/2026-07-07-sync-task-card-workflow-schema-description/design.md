## Context

`tools/dataspec-mcp.mjs` 的 `create_task_card` tool schema 里，`workflowId.description` 手写了 recipe id 列表。新增 `standard-evidence-review` 后，实际 `supportedWorkflowRecipeIds()` 已包含该 id，但 schema description 没有同步。这个字段是 MCP client 选择参数时最先读取的 AI-facing contract，适合从同一来源生成。

## Goals / Non-Goals

**Goals:**

- 让 `create_task_card.workflowId.description` 自动反映当前 `WORKFLOW_RECIPES` 支持的 id。
- 增加测试，防止以后新增 recipe 时 MCP schema description 再次漂移。

**Non-Goals:**

- 不新增或修改 MCP tool 名称、入参字段、返回字段。
- 不改变 task card 创建、渲染、敏感输入拒绝或 workflow 校验逻辑。
- 不更新 CLI/README；上一项已同步用户文档，本项只修 MCP schema 文案来源。

## Decisions

1. **使用 `supportedWorkflowRecipeIds().join('、')` 动态生成 description。**
   - 原因：`supportedWorkflowRecipeIds()` 已是 task card 校验错误消息使用的统一来源，复用它可避免双份列表。

2. **测试读取 `tools/list` 的真实 schema。**
   - 原因：这比单测 helper 更接近 MCP client 实际看到的契约。

## Risks / Trade-offs

- **[Risk] description 动态生成导致测试顺序敏感** -> Mitigation：测试按 `supportedWorkflowRecipeIds()` 逐项断言存在，不绑定完整中文句子。
- **[Risk] 误以为新增可执行行为** -> Mitigation：只改 schema description 和测试，不修改 handler 的 tool call 分支。
