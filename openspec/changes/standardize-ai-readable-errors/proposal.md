## Why

DataSpec 已经服务前端、CLI 和 MCP，但失败时多数入口只返回人类 message。AI agent 遇到服务未启动、token 无效、projectId 错误、SQL/数据库输入错误时，缺少稳定错误码、分类、可重试性和下一步动作，容易盲目重试或给出错误建议。

## What Changes

- 在后端统一响应中新增可选 `error` 诊断对象，保留现有 `code/message/data` 兼容字段。
- 为常见错误生成稳定的 `code/category/retryable/suggestedAction/docsRef`。
- CLI 在 DataSpec API 失败时输出可机器读取的 `DataSpecError` 诊断行。
- MCP 在 JSON-RPC error `data` 中透出同一类诊断对象。
- 新增后端、CLI、MCP 测试，锁定错误契约第一版字段。
- 更新 README 和 TODO，说明 P6-18 第一版能力和边界。

## Capabilities

### New Capabilities

- `ai-readable-error-contract`: 面向 AI/CLI/MCP 的稳定错误诊断契约。

### Modified Capabilities

- 无。

## Impact

- 后端：新增错误诊断模型/分类器，更新 `R.fail` 和全局异常处理。
- CLI/MCP：解析后端错误诊断并透出给调用方。
- 测试：新增后端单测、CLI/MCP 错误契约测试。
- 文档：README/TODO 同步能力说明。
