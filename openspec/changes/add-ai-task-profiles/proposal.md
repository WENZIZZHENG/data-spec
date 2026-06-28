## Why

DataSpec 已经能给 AI 提供字段、规则、workflow recipe、fixedSql 策略和按需 Context，但不同任务仍需要人工在 prompt 里临时说明“该读哪些标准、该用什么输出格式、是否只做 dry-run”。P6-36 需要把这些偏好沉淀为项目级 AI task profile，让 CLI、MCP、前端和 doctor 都能稳定读取同一份任务模式。

## What Changes

- 新增项目级 AI task profile 模型，内置 `create-table`、`sql-fix`、`reverse-import`、`pr-review` 等默认模式。
- 新增只读/轻量配置 API，用于读取项目 profile 列表、默认 profile 和单个任务模式详情。
- 扩展仓库级 `.dataspec/config.json`，允许声明默认 `aiProfile` / `taskType`，CLI 和 MCP 可据此选择上下文范围与输出格式。
- 扩展 CLI/MCP：提供查看 profile 的命令/resource/prompt 指引，并让 lint/export-context 等入口可读取 profile 默认参数。
- 扩展 `dataspec doctor`，诊断 profile 缺失、未知 taskType、引用的规则/范围不可用，并输出 AI 可读 next action。
- 前端新增 AI profile 查看与切换入口，展示 taskType、contextScope、ruleset、fixedSqlPolicy、outputFormat、maxContextFields 和推荐命令。
- 不新增外部 LLM provider 配置，不做复杂权限或团队级审批，不把 profile 当作强制执行的安全边界。

## Capabilities

### New Capabilities

- `ai-task-profiles`: 项目级 AI 使用画像与任务模式配置，覆盖 profile 数据结构、API、前端展示和 AI 消费语义。

### Modified Capabilities

- `dataspec-local-config`: `.dataspec/config.json` 支持声明默认 AI profile 和 task type。
- `dataspec-doctor`: doctor 输出 profile 诊断与修复建议。
- `dataspec-cli`: CLI 能读取、展示并在相关命令中使用 profile 默认参数。
- `dataspec-mcp`: MCP 暴露 profile resource/prompt，使 AI 客户端可读取当前任务模式。
- `ai-context-scoped-export`: scoped context 可由 profile 默认 scope、query、limit 驱动。

## Impact

- 后端：新增 AI profile DTO/service/controller，复用项目字段、规则、workflow 和 fixedSql 策略能力，不新增数据库迁移。
- 前端：新增 profile 页面或工作台分区，并补 smoke 测试。
- CLI/MCP：扩展 config 解析、doctor、profile 命令/resource 和相关测试。
- 文档/契约：更新 OpenSpec、README、AI contract 或 TODO 状态。
