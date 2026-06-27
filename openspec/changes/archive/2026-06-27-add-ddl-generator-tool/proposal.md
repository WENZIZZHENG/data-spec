## Why

AI 不应总是直接手写建表 SQL。DataSpec 已有表模板、模板字段和 lint 引擎，本阶段先提供按模板生成 PostgreSQL DDL 的能力，并在生成后立即自检，作为 API、CLI、MCP 的统一 AI tool。

## What Changes

- 新增 DDL 生成 service：按模板字段生成 `CREATE TABLE` 和 `COMMENT ON`。
- 新增 API：`GET /api/generator/ddl/preview`。
- 新增 CLI 命令：`generate-ddl --template <id> --table <name> --project <id> --format json`。
- 新增 MCP tool：`generate_table_ddl`。
- 更新 OpenAPI TS schema、前端 generator API wrapper、README/TODO。

## Capabilities

### New Capabilities

- `ddl-generator-tool`: 基于表模板生成 PostgreSQL DDL 并返回 lint 自检结果。

### Modified Capabilities

- `dataspec-cli`: 暴露 DDL 生成命令。
- `dataspec-mcp`: 暴露 DDL 生成 tool。

## Impact

- 后端 `generator` 增加 DDL service/controller API。
- CLI/MCP 通过同一 HTTP API 复用后端生成逻辑。
- 不执行数据库变更，不做 migration planner，不自动写入用户文件。
