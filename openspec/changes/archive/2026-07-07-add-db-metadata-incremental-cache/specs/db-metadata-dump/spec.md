## ADDED Requirements

### Requirement: schema dump 携带 metadata fingerprint
DataSpec SHALL include cache and fingerprint metadata in database schema dumps while preserving the schema-only dump contract.

#### Scenario: dump 返回缓存来源信息
- **WHEN** DataSpec returns a database schema dump from direct metadata or cache
- **THEN** the dump SHALL include `metadataFingerprint`, source database product/version, cache status, `lastSeenAt`, `expiresAt`, and `refreshMode`
- **AND** the dump MUST remain free of passwords, API tokens, bearer tokens, full JDBC URLs, connection strings, and source database row values.

#### Scenario: 缓存 dump 可复用到分析链路
- **WHEN** a schema dump is built from fresh cache entries
- **THEN** DataSpec SHALL convert it to the same parsed table definition model used by reverse import, compare, and coverage analysis
- **AND** downstream analysis SHALL be able to reference the same `metadataFingerprint`.

### Requirement: schema dump 刷新差异摘要
DataSpec SHALL attach bounded schema-only change summaries to dumps produced by cache refresh.

#### Scenario: dump 刷新后发现结构变化
- **WHEN** a refreshed dump differs from the previous cached snapshot
- **THEN** the dump SHALL include changed table and field summary information
- **AND** the summary SHALL identify added, removed, and changed fields without exposing business row values.
