## 1. OpenSpec 与范围确认

- [x] 1.1 校验 proposal、design 和 spec delta 一致，确认只修改 `ai-workflow-recipes` 可见 recipe catalog。
- [x] 1.2 自查不新增真实 CLI command、MCP resource/tool 或后端 API。

## 2. Recipe 实现

- [x] 2.1 在 `tools/dataspec-workflows.mjs` 新增 `standard-evidence-review` recipe，包含输入、prechecks、步骤、产物、失败恢复、nextActions 和 `plan-only` 边界。
- [x] 2.2 更新 CLI workflow list/show 测试，锁定新增 recipe 的 id、required inputs、只读 API 步骤和无独立 CLI/MCP surface。
- [x] 2.3 更新 MCP workflow resource 测试，确认新增 recipe 通过 MCP resource 可发现。
- [x] 2.4 更新 task-card 测试，确认新增 recipe 可派生任务卡且缺少必填输入时会 BLOCKED。
- [x] 2.5 更新 README 的 workflow recipe 列表和示例命令。

## 3. 验证与收口

- [x] 3.1 运行 workflow/task-card/MCP 定点 Node 测试。
- [x] 3.2 运行 `openspec validate add-standard-evidence-review-workflow --strict`。
- [x] 3.3 运行 `git diff --check` 和敏感词扫描。
- [x] 3.4 启动独立子 agent 只读评审 workflow recipe、CLI/MCP 外部协议、安全边界和测试；关闭子 agent 并处理 findings。
- [x] 3.5 追加 `Verification Evidence` 并创建本地 commit。

## Verification Evidence

- 2026-07-07：先运行红灯验证：
  - `node --test tools/dataspec-cli.test.mjs --test-name-pattern "workflow"` 失败 2 项，原因是 `standard-evidence-review` 尚未存在。
  - `node --test tools/dataspec-mcp.test.mjs --test-name-pattern "workflow recipes"` 失败 1 项，原因是 MCP workflow resource 尚未列出新 recipe。
  - `node --test tools/dataspec-task-card.test.mjs` 失败 2 项，原因是 task card 尚不支持新 recipe。
- 2026-07-07：补充 recipe 后运行 `node --test tools/dataspec-cli.test.mjs --test-name-pattern "workflow|task-card"` 通过，结果为 140 pass、0 fail、2 skipped（Windows symlink 权限相关跳过）。
- 2026-07-07：`node --test tools/dataspec-mcp.test.mjs --test-name-pattern "workflow recipes|task card tools"` 通过，结果为 40 pass、0 fail。
- 2026-07-07：`node --test tools/dataspec-task-card.test.mjs` 通过，结果为 7 pass、0 fail。
- 2026-07-07：`node --test tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，结果为 21 pass、0 fail。
- 2026-07-07：`openspec validate add-standard-evidence-review-workflow --strict` 通过。
- 2026-07-07：`git diff --check` 通过；新增行敏感词扫描命中 README/OpenSpec 的安全边界说明和扫描命令文本，人工确认不是 raw secret、Authorization header、JDBC URL、DSN 或业务数据行。
- 2026-07-07：独立只读评审子 agent `019f3a27-6036-7ce1-b5bf-d0912b659464`（Mill）用于评审 workflow recipe、CLI/MCP 外部协议、安全边界和测试，结论为通过，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
- 2026-07-07：本地 commit 由本次验证收口后的 Git 提交动作完成，最终 commit hash 以仓库 Git history 为准。
