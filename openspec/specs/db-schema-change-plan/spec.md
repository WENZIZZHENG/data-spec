# db-schema-change-plan Specification

## Purpose
定义 DataSpec 如何基于只读数据库元数据和当前字段标准生成 schema 变更计划预览，用于评估迁移 SQL、风险、回滚提示和人工检查项，而不修改数据库或字段目录。
## Requirements
### Requirement: 数据库 schema change plan 只读契约
DataSpec SHALL generate a read-only database schema change plan from selected database metadata and current project field standards.

#### Scenario: 生成计划摘要
- **WHEN** a caller submits database connection information and selected table names to the schema plan API
- **THEN** DataSpec reads schema-only metadata through the existing database metadata flow
- **AND** returns `currentSchemaHash`, `targetSpecHash`, `riskLevel`, `changeSet`, `migrationSql`, `rollbackHint`, `manualChecks`, `blockedReasons`, and `nextActions`
- **AND** does not modify the source database or DataSpec field catalog.

#### Scenario: 计划不包含敏感信息
- **WHEN** DataSpec returns a schema change plan
- **THEN** the response MUST NOT include raw database passwords, bearer tokens, API tokens, full JDBC URLs, DSNs, or source database row values.

### Requirement: 字段差异到迁移草案
DataSpec SHALL translate database compare field differences into auditable schema change items and dry-run SQL draft text.

#### Scenario: 缺注释字段生成注释草案
- **WHEN** a matched database column is missing a comment and the standard field has a comment
- **THEN** the plan includes a change item with action `ALTER_COMMENT`
- **AND** `migrationSql` includes a comment statement draft for that column.

#### Scenario: 属性变化字段生成人工确认项
- **WHEN** a matched database column differs from the standard field in type, nullable, default value, or comment
- **THEN** the plan includes one or more change items with action `ALTER_COLUMN`
- **AND** each item lists current value, target value, risk level, and manual checks needed before execution.

#### Scenario: 未命中标准字段默认阻塞删除
- **WHEN** a database column does not match any standard field name or alias
- **THEN** the plan marks the item as a high-risk `DROP_CANDIDATE`
- **AND** the plan adds blocked reasons or manual checks instead of producing an executable destructive SQL statement.

### Requirement: API CLI and frontend preview
DataSpec SHALL expose the same schema change plan through API, CLI JSON output, and the reverse import frontend workflow.

#### Scenario: CLI 输出 JSON 计划
- **WHEN** a user runs the schema plan CLI command with project and database connection options
- **THEN** the CLI calls the schema plan API
- **AND** prints the unwrapped JSON plan without executing migration SQL.

#### Scenario: 前端显示风险预览
- **WHEN** a user generates a schema plan from the reverse import page
- **THEN** the page displays the overall risk level, blocked reasons, manual checks, change items grouped by table and dry-run SQL
- **AND** high-risk drop or rename-like items are visually distinguishable from safe comment changes.

#### Scenario: AI 可读取下一步建议
- **WHEN** an AI reads the schema plan JSON
- **THEN** it can use `nextActions`, `manualChecks`, and `blockedReasons` to decide whether to update standards, generate a reviewed migration file, or stop for human confirmation.
