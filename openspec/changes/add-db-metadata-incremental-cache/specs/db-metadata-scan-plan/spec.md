## ADDED Requirements

### Requirement: scan plan 暴露 metadata cache 状态
DataSpec SHALL include metadata cache freshness information in database metadata scan results without changing the existing pagination contract.

#### Scenario: 扫描结果包含缓存摘要
- **WHEN** a user scans database metadata with `pageSize` and optional `cursor`
- **THEN** the scan result SHALL still include `scanId`, `estimatedTableCount`, current page tables, `cursor`, `progress`, `partialSummary`, `resumeCommand`, and `cancelled`
- **AND** it SHALL include a cache summary with `metadataFingerprint`, `cacheHit`, `stale`, `lastSeenAt`, `expiresAt`, `refreshMode`, and safe next actions when cache evidence is available.

#### Scenario: 恢复命令包含无敏信息缓存提示
- **WHEN** DataSpec returns a scan `resumeCommand`
- **THEN** the command SHALL preserve cursor and page size recovery information
- **AND** it MAY include the current `metadataFingerprint`
- **AND** it MUST NOT contain passwords, API tokens, bearer tokens, full JDBC URLs, connection strings, or source database row values.

### Requirement: scan 刷新策略与分页兼容
DataSpec SHALL apply metadata cache mode to scan requests without introducing background full-database scans.

#### Scenario: scan 自动模式不读取非当前页列 metadata
- **WHEN** a scan request uses `metadataCacheMode=AUTO`
- **THEN** DataSpec SHALL NOT read column metadata for tables outside the current page
- **AND** it SHALL NOT default to a full database column scan just to populate cache.

#### Scenario: scan 强制刷新当前选择
- **WHEN** a scan request uses `metadataCacheMode=REFRESH` and includes selected table names
- **THEN** DataSpec SHALL refresh cache only for the selected or current-page tables that require column metadata
- **AND** it SHALL report any change summary for those refreshed tables.
