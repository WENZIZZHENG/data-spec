## Why

SQL lint 结果目前只能定位到表名和字段名，前端、CLI、MCP、PR Review 都无法稳定指出具体行列。P4 下一轮优先要把问题反馈变成可点击、可执行的定位信息，降低人工和 AI 修复成本。

## What Changes

- `LintIssue` 增加 `line`、`column`、`sourceStart`、`sourceEnd` 定位字段。
- 后端 lint 服务在规则执行后，基于原始 SQL、表名和字段名为问题回填第一版 source span。
- `/api/lint`、检查记录详情、CLI/MCP/PR Review 复用同一结构化定位字段。
- 前端 SQL 校验页展示位置，并支持点击问题跳转到 Monaco 编辑器对应行列。

## Capabilities

### New Capabilities
- `sql-issue-source-span`: SQL lint issue 的行列定位和 source span 契约。

### Modified Capabilities
- 无。

## Impact

- 后端 lint model、lint service、检查记录序列化 JSON。
- 前端 OpenAPI 生成类型或手动类型补充、SQL 校验页问题列表。
- 相关单元测试和 TODO 路线图状态。
