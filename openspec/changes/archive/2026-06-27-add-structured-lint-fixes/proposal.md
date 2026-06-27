## Why

AI agent 需要可执行的修复线索，而不是只读自然语言 lint 文案。P1-6 将 lint issue 扩展为结构化建议，先让核心规则输出 `suggestion`、`replacement`、`before`、`after`、`confidence`，为后续修正版 SQL 输出打基础。

## What Changes

- 扩展 `LintIssue` 输出结构，增加结构化修复建议字段。
- snake_case、推荐字段名、禁用字段名、必备列、后缀/前缀类型规则输出可机器读取的建议。
- 更新 OpenAPI TS schema，并在 SQL 校验页展示建议列。
- 更新 README/TODO。

## Capabilities

### New Capabilities

- `structured-lint-fixes`: SQL lint issue 携带结构化修复建议。

### Modified Capabilities

- `sql-lint`: 核心规则输出更适合 AI 消费的修复元数据。

## Impact

- 修改 lint model 和部分规则实现。
- CLI/MCP 自动透传新的 lint JSON 字段。
- 不自动改写用户 SQL，不新增检查记录表。
