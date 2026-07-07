## 1. OpenSpec 与测试准备

- [x] 1.1 校验 proposal、design 和 spec delta 一致，确认只修改 `dataspec-mcp` 的 task card tool schema description。
- [x] 1.2 先补 MCP `tools/list` 失败测试，覆盖 `create_task_card.workflowId.description` 包含当前支持的 workflow recipe ids。

## 2. 实现

- [x] 2.1 更新 `tools/dataspec-mcp.mjs`，让 workflow id schema description 从 `supportedWorkflowRecipeIds()` 生成。
- [x] 2.2 自查未修改 MCP tool 入参字段、返回结构、handler 分支或 task-card 执行行为。

## 3. 验证与收口

- [x] 3.1 运行 MCP 定点 Node 测试。
- [x] 3.2 运行 `openspec validate sync-task-card-workflow-schema-description --strict`。
- [x] 3.3 运行 `git diff --check` 和敏感词扫描。
- [x] 3.4 启动独立子 agent 只读评审 MCP schema description、外部协议边界、安全边界和测试；关闭子 agent 并处理 findings。
- [x] 3.5 追加 `Verification Evidence` 并创建本地 commit。

## Verification Evidence

- 2026-07-07：先运行 `node --test tools/dataspec-mcp.test.mjs --test-name-pattern "task card tools create"`，红灯 1 项，原因是 `create_task_card.workflowId.description` 仍只列旧 recipe，缺少 `standard-evidence-review`。
- 2026-07-07：改为从 `supportedWorkflowRecipeIds()` 生成 description 后，重跑 `node --test tools/dataspec-mcp.test.mjs --test-name-pattern "task card tools create"` 通过，结果为 40 pass、0 fail。
- 2026-07-07：`node --test tools/dataspec-mcp.test.mjs` 通过，结果为 40 pass、0 fail。
- 2026-07-07：`node --test tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，结果为 21 pass、0 fail。
- 2026-07-07：`openspec validate sync-task-card-workflow-schema-description --strict` 通过。
- 2026-07-07：`git diff --check` 通过；新增行敏感词扫描无匹配。
- 2026-07-07：独立只读评审子 agent `019f3a32-5815-7a81-be88-55aeecdffb54`（Descartes）用于评审 MCP schema description、外部协议边界、安全边界和测试，结论为通过，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
- 2026-07-07：本地 commit 由本次验证收口后的 Git 提交动作完成，最终 commit hash 以仓库 Git history 为准。
