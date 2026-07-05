# database-candidate Specification

## Purpose
TBD - created by archiving change add-database-candidate. Update Purpose after archive.
## Requirements
### Requirement: 数据库 metadata 浏览器
DataSpec SHALL allow users to browse selected database schema metadata before importing candidates.

#### Scenario: 浏览选中表结构
- **WHEN** a user submits a supported database connection and selected table names to the metadata browser API
- **THEN** DataSpec SHALL return table, column, comment, type, nullable, default, and index metadata for the selected tables
- **AND** the browser response SHALL include summary counts for tables, columns, indexes, candidate fields, missing comments, changed fields, and unmanaged fields.

#### Scenario: 搜索 metadata
- **WHEN** the browser result is shown in the frontend
- **THEN** users SHALL be able to filter metadata rows by schema, table, column, comment, type, index name, or standard match text
- **AND** current candidate selections SHALL remain stable while filters change.

### Requirement: 字段标准匹配与候选选择
DataSpec SHALL show standard matching and candidate status while browsing metadata.

#### Scenario: 展示字段分析状态
- **WHEN** selected tables are browsed
- **THEN** each browsed column SHALL show its standard match status when available
- **AND** columns with missing comments, type differences, unmanaged status, or import candidate status SHALL be distinguishable in the browser response and UI.

#### Scenario: 浏览器选择候选
- **WHEN** the browser response includes import candidates
- **THEN** the frontend SHALL let users select or clear candidate fields from the metadata browser
- **AND** the existing confirm import flow SHALL submit only selected candidates and preserve ignored-candidate behavior.

### Requirement: AI 可读结构摘要
DataSpec SHALL provide a schema-only summary suitable for AI handoff.

#### Scenario: 读取选中表摘要
- **WHEN** selected tables are browsed
- **THEN** DataSpec SHALL return an AI-readable summary containing selected table names, columns, index names, coverage/candidate counts, and next actions
- **AND** the summary MUST NOT contain passwords, API tokens, bearer tokens, full JDBC URLs, or source database row values.

### Requirement: 只读安全边界
DataSpec SHALL keep database metadata browsing read-only.

#### Scenario: 浏览不写入源库或标准库
- **WHEN** a user browses database metadata
- **THEN** DataSpec SHALL NOT execute arbitrary user SQL
- **AND** DataSpec SHALL NOT write to the source database
- **AND** DataSpec SHALL NOT write to the standard field library unless the user later runs the existing explicit confirm import action.
