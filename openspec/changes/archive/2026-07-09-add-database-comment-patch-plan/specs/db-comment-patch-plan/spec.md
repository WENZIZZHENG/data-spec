## ADDED Requirements

### Requirement: 数据库 COMMENT patch plan 只读契约
DataSpec SHALL generate a project-scoped, read-only database COMMENT patch plan from selected schema-only database metadata and current DataSpec standards.

#### Scenario: 生成 COMMENT 回写计划
- **WHEN** a caller submits `projectId`, database connection metadata, and selected table names to the comment patch plan API
- **THEN** DataSpec reads table and column metadata through the existing read-only metadata flow
- **AND** returns `kind`, `schemaVersion`, `projectId`, `databaseType`, `metadataFingerprint`, `planHash`, `summary`, `items`, `dryRunSql`, `dialectSupport`, `riskLevel`, `rollbackHint`, `evidence`, `safety`, and `nextActions`
- **AND** it MUST NOT modify the source database, DataSpec field catalog, standard candidates, task runs, local files, or external services.

#### Scenario: 空差异返回 no-op 计划
- **WHEN** selected database table and column comments already match DataSpec table and field standards
- **THEN** the plan returns zero executable comment changes
- **AND** `summary.noOpCount` reflects the matched items
- **AND** `dryRunSql` is empty or contains only non-executable explanatory comments.

### Requirement: COMMENT 差异与方言 SQL 草稿
DataSpec SHALL represent table and column COMMENT differences as auditable plan items and generate dialect-aware dry-run SQL only when safe.

#### Scenario: PostgreSQL 注释草稿
- **WHEN** a PostgreSQL table or column has `status=MISSING` or `status=CHANGED`
- **THEN** the item includes `objectType`, `schemaName`, `tableName`, optional `columnName`, `currentComment`, `targetComment`, `commentDiff`, `riskLevel`, `rollbackHint`, and `dryRunSql`
- **AND** PostgreSQL dry-run SQL uses `COMMENT ON TABLE` or `COMMENT ON COLUMN` statements with escaped comment literals.

#### Scenario: MySQL 注释草稿
- **WHEN** a MySQL table comment can be updated safely
- **THEN** the plan uses `ALTER TABLE ... COMMENT = ...` as dry-run SQL.
- **WHEN** a MySQL column comment requires a full column definition that DataSpec cannot safely reconstruct from metadata
- **THEN** the item status MUST be `UNSUPPORTED`
- **AND** it MUST include manual next actions instead of unsafe SQL.

#### Scenario: 高风险或不支持项阻止可执行 SQL
- **WHEN** a comment item is ambiguous, unsupported, missing a target standard, or lacks enough metadata to preserve source column attributes
- **THEN** the item risk is `MEDIUM` or `HIGH`
- **AND** `dryRunSql` for that item is omitted
- **AND** `blockedReasons` or `manualChecks` explain what the user must review.

### Requirement: COMMENT patch plan 安全证据
DataSpec SHALL keep COMMENT patch plan output safe for AI-assisted review and evidence export.

#### Scenario: 输出不泄漏敏感信息
- **WHEN** DataSpec returns a comment patch plan, error, evidence, route, command, or rollback hint
- **THEN** the output MUST NOT include database passwords, API tokens, bearer tokens, Authorization headers, full JDBC URLs, DSNs, connection strings, URL userinfo, or source database row values.

#### Scenario: 证据可复核
- **WHEN** a plan includes executable or blocked comment items
- **THEN** evidence SHALL include selected schema/table scope, metadata fingerprint when available, standard field references, normalized input summary, safety flags, and validation next actions
- **AND** evidence SHALL be sufficient for a reviewer or AI agent to explain why each comment change is proposed without reconnecting to the source database.
