## Why

DataSpec 已能导出完整 AI Context 包，但字段、规则、枚举和历史记录变多后，AI agent 每次读取完整包会浪费上下文，也容易把当前 SQL 或需求无关的标准混入判断。P6-7 需要提供稳定的按需裁剪入口，让个人/小团队在建表、修 SQL 和字段设计时获取最小可用字段标准。

## What Changes

- 新增 AI Context 裁剪参数，支持按字段名/别名/显示名、分类/数据域、状态、表名或任务描述做确定性过滤。
- 字段目录和 zip 包在裁剪后仍保持原 JSON Schema 兼容，并额外输出 `contextScope` 元数据，记录裁剪条件、命中字段数、总字段数、命中原因和缺失提示。
- CLI `export-context` 增加 `--scope`、`--query`、`--status`、`--limit` 参数。
- MCP 增加面向当前任务的 `search_field_catalog` tool，并允许 `get_field_catalog` 传入裁剪参数。
- 前端 AI Context 页面增加按需导出控件和裁剪预览。
- 完整导出保持默认行为，不改变现有调用方。

## Capabilities

### New Capabilities

- `ai-context-scoped-export`: 定义 DataSpec 按需裁剪 AI Context 字段目录、zip 包、CLI 和 MCP 检索入口的可观察行为。

### Modified Capabilities

- `ai-context-package`: 保持完整包兼容，同时允许可选裁剪元数据。

## Impact

- 后端：扩展 `AiContextExportService`、`AiContextController`，新增裁剪请求/结果模型。
- CLI/MCP：扩展 `tools/dataspec-cli.mjs` 和 `tools/dataspec-mcp.mjs` 的参数与测试。
- 前端：扩展 AI Context 页面和 API wrapper。
- 文档：更新 README、TODO 和 `.dataspec/README.md` 导出内容。
