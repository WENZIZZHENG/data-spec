## Why

DataSpec 已经支持 DDL 生成、SQL 检查和数据库直连反向导入，但用户真正准备落库前还缺少一份可审计的 schema change plan。AI 或用户不能只拿到一段不可追溯的 `ALTER` SQL，而需要同时看到当前库结构、目标标准、风险、人工确认点和回滚提示。

## What Changes

- 新增只读数据库 Schema 变更计划能力：基于当前数据库 schema metadata 与 DataSpec 标准比对结果生成 `currentSchemaHash`、`targetSpecHash`、`changeSet`、`riskLevel`、`migrationSql`、`rollbackHint`、`manualChecks`、`blockedReasons` 和 `nextActions`。
- 新增数据库直连 API：`POST /api/reverse-import/database/schema-plan`，复用现有 `DatabaseConnectionReq`，只读取 metadata，不执行迁移、不写源库、不保存连接密码。
- 新增 CLI 入口：输出适合 AI/CI 读取的 JSON 计划，默认只预览，不生成或执行迁移文件。
- 在反向导入前端页面增加 schema plan 预览区，突出高风险 drop/rename/manual review 和不可自动执行边界。
- 更新 README/TODO/OpenSpec evidence，说明第一版只生成迁移草案和风险说明，不替代 Flyway/Liquibase/Atlas 等迁移工具。

## Capabilities

### New Capabilities

- `db-schema-change-plan`: 数据库 schema 变更计划预览，覆盖只读计划 API、CLI JSON 输出、前端风险预览和 AI 可读字段契约。

### Modified Capabilities

- `db-reverse-import-compare`: 在现有数据库直连比对基础上增加 schema plan 入口，要求计划复用 compare/browse 的 metadata 和标准差异，不改变已有 compare 响应语义。

## Impact

- 后端：`dataspec-server` 新增 schema plan response model、service 方法和 controller endpoint；复用现有 metadata dump/compare 读取链路。
- 前端：`dataspec-web` 新增 API wrapper、类型、反向导入页 schema plan 预览 UI 和源码级冒烟测试覆盖。
- CLI/tools：`tools/dataspec-cli.mjs` 新增只读命令和契约 fixture/test。
- 文档与规格：更新 README、TODO、OpenSpec delta 和 Verification Evidence。
- 安全边界：不新增数据库写入，不保存 raw password、JDBC URL、DSN、token 或业务数据行；输出 SQL 是 dry-run 草案，包含 manualChecks/blockedReasons。
