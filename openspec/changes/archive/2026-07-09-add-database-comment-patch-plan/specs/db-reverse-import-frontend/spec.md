## ADDED Requirements

### Requirement: 反向导入页展示 COMMENT 回写计划
The reverse import page SHALL expose a database COMMENT patch plan preview in database direct mode without executing source database writes.

#### Scenario: 生成 COMMENT 回写计划
- **WHEN** a user selects database tables in reverse import database direct mode and clicks the COMMENT plan action
- **THEN** the page calls the comment patch plan API with the current project, non-sensitive connection metadata, selected table names, and metadata cache mode
- **AND** it displays summary counts, risk level, dialect support, metadata fingerprint, blocked reasons, and next actions.

#### Scenario: 展示 dry-run SQL 和差异项
- **WHEN** the API returns COMMENT patch plan items
- **THEN** the page groups table and column comment differences by table
- **AND** shows `currentComment`, `targetComment`, `commentDiff`, item status, risk level, rollback hint, and safe `dryRunSql` when available.

#### Scenario: 前端不泄漏敏感信息
- **WHEN** the page renders plan output, errors, copied SQL, copied JSON, or evidence text
- **THEN** rendered and copied content MUST NOT include password, token, Authorization header, complete JDBC URL, DSN, connection string, URL userinfo, or source database row values.
