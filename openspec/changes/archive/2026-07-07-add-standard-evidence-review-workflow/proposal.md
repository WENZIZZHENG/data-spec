## Why

`standard-evidence` 已经能被 capability catalog、session bootstrap、AI Context 和 CLI fixture 发现，但 workflow recipes 仍缺少一条“先看字段标准证据再回答/修改”的任务路径。AI 在解释字段来源、可信度、最近使用情况或准备调整标准时，容易直接跳到生成建议，缺少可复现的证据检查顺序。

## What Changes

- 新增 `standard-evidence-review` workflow recipe，指导 AI 或开发者按项目和字段主体读取标准证据。
- 让 CLI `workflow list/show`、MCP workflow resource、AI Context workflows markdown 和 task card 派生都能发现该 recipe。
- 保持 recipe 为 plan-only：只输出步骤和命令建议，不自动调用 API、不写项目、不新增 CLI command、MCP tool 或后端 route。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `ai-workflow-recipes`: supported workflow recipe catalog 增加 `standard-evidence-review`，并要求其保留只读、plan-only、安全恢复边界。

## Impact

- Tools：更新 `tools/dataspec-workflows.mjs`，以及 CLI/MCP/task-card 相关测试。
- OpenSpec：修改 `ai-workflow-recipes` 规格，记录新增 recipe 的可观察行为。
- 安全：recipe 只描述读取证据和记录结论的步骤；不包含 raw SQL、AI payload、候选 raw evidence、raw source metadata、token、password、Authorization、JDBC URL、DSN 或业务数据行。
- 验证：运行 workflow/task-card/MCP 定点测试、OpenSpec strict、`git diff --check`、敏感词扫描和独立子 agent 评审。
