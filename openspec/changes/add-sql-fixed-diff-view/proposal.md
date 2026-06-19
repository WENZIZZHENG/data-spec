## Why

`fixedSql` 已能给出完整修正 SQL，但用户和 AI 更需要知道“具体改了哪里”。P4-2 要把修正结果从整段文本升级为可读 diff，便于前端复核、CLI/PR Review 摘要和 AI 后续修改。

## What Changes

- `/api/lint` 的 `LintResult` 增加 `fixedSqlDiff`，当原 SQL 与修正 SQL 不同时返回 unified diff 文本。
- 后端新增轻量 diff 生成器，不引入额外依赖。
- 前端 SQL 校验页在修正 SQL 面板中展示差异视图，保留复制完整修正 SQL。
- 检查记录详情基于原 SQL 与修正 SQL展示同样的差异视图。

## Capabilities

### New Capabilities
- `sql-fixed-diff`: SQL 修正结果 diff 输出与页面展示。

### Modified Capabilities
- 无。

## Impact

- 后端 lint model、lint service、OpenAPI schema。
- 前端 SQL 校验页。
- TODO 路线图与 OpenSpec change。
