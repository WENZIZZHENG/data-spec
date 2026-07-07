# sql-fixed-diff Specification

## Purpose
定义 fixedSql 的 unified diff 输出和前端展示能力，让用户在复制或应用自动修复前能直观看到 SQL 变更。
## Requirements
### Requirement: Fixed SQL Diff Output
The system SHALL include a unified diff for fixed SQL when a deterministic fixed SQL candidate differs from the original SQL.

#### Scenario: Return fixed SQL diff
- **WHEN** a client submits SQL that produces a non-empty `fixedSql` different from the original SQL
- **THEN** the lint response includes `fixedSqlDiff`
- **AND** `fixedSqlDiff` contains deletion lines, insertion lines, and context lines in unified diff style

#### Scenario: Omit empty diff
- **WHEN** `fixedSql` is empty or equivalent to the original SQL
- **THEN** `fixedSqlDiff` is absent or null

### Requirement: SQL Lint Page Diff View
The SQL lint page SHALL display fixed SQL differences when diff data is available.

#### Scenario: Show diff for current lint result
- **WHEN** lint result contains `fixedSqlDiff`
- **THEN** the page displays a diff view under the fixed SQL controls
- **AND** added, removed, and context lines are visually distinguishable

#### Scenario: Show diff for record details
- **WHEN** a SQL check record has both original SQL and fixed SQL
- **THEN** the record detail dialog displays a diff view

#### Scenario: Keep copy fixed SQL
- **WHEN** the page displays a diff view
- **THEN** the existing copy fixed SQL action remains available
