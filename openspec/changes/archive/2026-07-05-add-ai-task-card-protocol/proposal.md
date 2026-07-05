## Why

AI 执行 DataSpec 工作流时已经知道“应该怎么做”，但缺少一张机器可读任务卡来记录当前目标、输入、步骤、允许动作、产物、恢复命令和停止条件。长任务一旦失败或切换会话，AI 容易重复写入、跳步或越过只读边界。

## What Changes

- 新增轻量 AI task card 协议，提供 JSON 与 Markdown 两种呈现形态。
- 从已有 workflow recipes 生成初始任务卡，覆盖建表、反向导入、PR SQL Review 和最小 Context 导出等高频任务。
- CLI 新增 task-card 相关命令，用于创建、展示和更新本地任务卡；所有写入仅限用户指定的任务卡文件。
- MCP 新增只读/本地工具入口，让 AI 客户端可创建或渲染任务卡，并读取下一步安全命令。
- 前端新增任务卡最小展示能力，用于在任务入口或结果页展示 goal、currentStep、allowedActions、artifacts、resumeCommand、validationCommands 和 stopConditions。
- 明确安全边界：任务卡不自动执行 workflow 步骤、不连接外部 LLM、不替代后端任务队列、不保存 token/password/JDBC URL。

## Capabilities

### New Capabilities

- `ai-task-card-protocol`: 定义 DataSpec AI task card 的字段、生命周期、JSON/Markdown 输出、安全边界和恢复语义。

### Modified Capabilities

- `ai-workflow-recipes`: workflow recipe 需要提供可生成任务卡的步骤、输入、产物和恢复建议。
- `dataspec-cli`: CLI 需要暴露 task-card 创建、展示和更新入口，输出稳定 JSON/Markdown。
- `dataspec-mcp`: MCP 需要暴露 task card tool，使 AI 客户端可生成或渲染任务卡。
- `frontend-task-entrypoints`: 前端任务入口需要能够展示任务卡摘要和恢复信息。

## Impact

- 后端：第一版不新增表和远端 API；复用已有 workflow recipe 与 AI task run 概念。
- CLI/MCP：新增本地 task-card 命令/tool 和 Node tests。
- 前端：新增任务卡展示工具函数或轻量组件，并接入现有任务入口 smoke test。
- 文档/TODO/OpenSpec：README、TODO 和 specs 更新；任务卡契约纳入后续 AI 写入安全与证据看板的基础。
