## ADDED Requirements

### Requirement: 覆盖率报告复用 metadata cache
DataSpec SHALL allow database direct field coverage reports to use fresh metadata cache entries instead of reconnecting for unchanged schema structures.

#### Scenario: 覆盖率报告使用新鲜缓存
- **WHEN** a database direct coverage request uses `metadataCacheMode=AUTO` and selected table cache entries are fresh
- **THEN** DataSpec SHALL generate the coverage report from cached schema-only table definitions
- **AND** it SHALL return the associated `metadataFingerprint` and cache status with the report.

#### Scenario: 覆盖率报告强制刷新
- **WHEN** a database direct coverage request uses `metadataCacheMode=REFRESH`
- **THEN** DataSpec SHALL refresh selected table metadata before generating coverage
- **AND** it SHALL return any schema-only change summary alongside the coverage report.

#### Scenario: 覆盖率缓存边界
- **WHEN** DataSpec generates a coverage report from metadata cache
- **THEN** the report SHALL analyze only table and column metadata
- **AND** it MUST NOT require or expose source database row values, passwords, tokens, full JDBC URLs, or connection strings.
