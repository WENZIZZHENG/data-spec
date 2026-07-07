## ADDED Requirements

### Requirement: 反向导入页展示 metadata cache 状态
反向导入页 SHALL display database metadata cache freshness, fingerprint, and refresh controls in database direct flows.

#### Scenario: 展示缓存命中和过期信息
- **WHEN** scan, browser, preview, compare, or import preparation returns metadata cache information
- **THEN** the page SHALL show whether the result came from fresh cache, stale cache, refresh, or bypass
- **AND** it SHALL show `lastSeenAt`, `expiresAt`, and `metadataFingerprint` when available.

#### Scenario: 用户手动刷新 metadata
- **WHEN** a user chooses to refresh database metadata
- **THEN** the next database metadata request SHALL send `metadataCacheMode=REFRESH`
- **AND** the UI SHALL keep existing selected table names unless the user explicitly clears them.

### Requirement: 反向导入页展示结构变化摘要
反向导入页 SHALL show schema-only metadata change summaries returned by backend refreshes.

#### Scenario: 刷新后展示字段变化
- **WHEN** a refresh response contains added, removed, or changed fields
- **THEN** the page SHALL display a bounded summary grouped by table
- **AND** it SHALL NOT display passwords, tokens, full JDBC URLs, connection strings, or source database row values.

#### Scenario: AI 摘要包含 fingerprint
- **WHEN** the user copies or views AI-readable database metadata context
- **THEN** the context SHALL include the metadata fingerprint and freshness state
- **AND** it SHALL remain sanitized for credential-like values.
