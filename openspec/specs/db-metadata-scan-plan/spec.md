# db-metadata-scan-plan Specification

## Purpose
TBD - created by archiving change add-database-scan-plan. Update Purpose after archive.
## Requirements
### Requirement: 数据库 metadata scan plan
DataSpec SHALL provide a read-only scan plan for large database metadata browsing.

#### Scenario: 分页读取表级 metadata
- **WHEN** a user submits a supported database connection with `pageSize` and no cursor
- **THEN** DataSpec SHALL return a scan result with `scanId`, `estimatedTableCount`, current page table entries, `cursor`, `progress`, `partialSummary`, `resumeCommand`, and `cancelled=false`
- **AND** the first page SHALL NOT require reading column metadata for tables outside the current page.

#### Scenario: 使用 cursor 继续下一批
- **WHEN** a user submits the returned cursor with the same connection context
- **THEN** DataSpec SHALL return the next page of table entries
- **AND** progress SHALL include processed table count, remaining table estimate, page size, and whether more pages are available.

### Requirement: 取消与恢复边界
DataSpec SHALL let users and AI stop a scan without causing writes or storing credentials.

#### Scenario: 取消扫描
- **WHEN** a scan request marks the scan as cancelled
- **THEN** DataSpec SHALL return `cancelled=true`
- **AND** DataSpec SHALL NOT write to the standard field library
- **AND** DataSpec SHALL NOT write to the source database
- **AND** DataSpec SHALL NOT continue scanning additional pages in the response.

#### Scenario: 恢复命令不含敏感信息
- **WHEN** DataSpec returns `resumeCommand`
- **THEN** the command SHALL include enough non-sensitive context for AI to request the next page
- **AND** it MUST NOT contain passwords, API tokens, bearer tokens, full JDBC URLs, or source database row values.

### Requirement: 分页预览与前端批次选择
DataSpec SHALL support partial preview from the current scan page.

#### Scenario: 当前页生成部分预览
- **WHEN** a user selects tables from the current scan page
- **THEN** the existing metadata browser or preview flow SHALL be able to run against only those selected table names
- **AND** unselected or not-yet-scanned tables SHALL NOT be imported.

#### Scenario: 前端按批次展示
- **WHEN** a scan result is shown in the reverse import page
- **THEN** users SHALL see current page tables, progress, next-page availability, cancel status, and current batch selection controls
- **AND** filters or page changes SHALL NOT drop already selected table names unless the user explicitly clears them.

### Requirement: AI 可发现扫描计划
DataSpec SHALL expose the database metadata scan plan in AI-facing capability discovery.

#### Scenario: Capability catalog 暴露 scan 入口
- **WHEN** an AI reads the `reverse-import` capability
- **THEN** the capability SHALL list `/api/reverse-import/database/scan` as an API endpoint
- **AND** it SHALL include scan-specific optional inputs such as `pageSize`, `cursor`, `scanId`, and `cancel`
- **AND** it SHALL reference the scan output contract so AI can discover the cursor, progress, resume command, and cancellation boundary.

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
