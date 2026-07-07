# db-metadata-incremental-cache Specification

## Purpose
定义 DataSpec 如何缓存数据库 schema-only 元数据快照、生成稳定 fingerprint，并在反向导入、扫描计划和覆盖率分析中安全复用缓存，避免重复全库扫描和敏感信息泄露。
## Requirements
### Requirement: 数据库 metadata 结构缓存
DataSpec SHALL persist database metadata cache entries as schema-only structure snapshots keyed by project, connection preset or source scope hash, schema, and table.

#### Scenario: 写入结构缓存
- **WHEN** DataSpec reads selected table metadata from a supported database connection
- **THEN** it SHALL persist cache entries containing table and column structure, index structure, source database product/version, `lastSeenAt`, `expiresAt`, and table fingerprint
- **AND** the cache entry MUST NOT contain passwords, API tokens, bearer tokens, full JDBC URLs, connection strings, or source database row values.

#### Scenario: 按来源边界隔离缓存
- **WHEN** two requests use different projects, connection presets, source scope hashes, schemas, or table names
- **THEN** DataSpec SHALL store and read their metadata cache entries independently
- **AND** one source SHALL NOT reuse another source's table structure.

### Requirement: 稳定 metadata fingerprint
DataSpec SHALL generate stable fingerprints from normalized database structure metadata so AI can decide whether downstream analysis must be rerun.

#### Scenario: 相同结构生成相同 fingerprint
- **WHEN** the same table structure is read multiple times with different JDBC result ordering
- **THEN** DataSpec SHALL normalize table, column, and index ordering before hashing
- **AND** it SHALL return the same table fingerprint and aggregate metadata fingerprint.

#### Scenario: 结构变化生成新 fingerprint
- **WHEN** a table column, data type, nullable flag, default value, comment, ordinal position, or index membership changes
- **THEN** DataSpec SHALL generate a different table fingerprint
- **AND** the aggregate metadata fingerprint for the selected tables SHALL change.

### Requirement: 缓存刷新策略
DataSpec SHALL support explicit metadata cache modes while preserving compatibility with existing database direct requests.

#### Scenario: 自动复用新鲜缓存
- **WHEN** a database direct request omits `metadataCacheMode` or sets it to `AUTO` and all selected tables have unexpired cache entries
- **THEN** DataSpec SHALL reuse the cached structure snapshots
- **AND** the response SHALL identify `cacheHit=true`, `stale=false`, `refreshMode=AUTO`, and the cache `lastSeenAt`.

#### Scenario: 强制刷新缓存
- **WHEN** a database direct request sets `metadataCacheMode=REFRESH`
- **THEN** DataSpec SHALL read metadata from the source database, update cache entries, and return `refreshMode=REFRESH`
- **AND** it SHALL compare the refreshed snapshot with the previous cached snapshot when one exists.

#### Scenario: 绕过缓存
- **WHEN** a database direct request sets `metadataCacheMode=BYPASS`
- **THEN** DataSpec SHALL read metadata from the source database for the current response
- **AND** it SHALL NOT read from or write to metadata cache for that request.

### Requirement: 结构变更摘要
DataSpec SHALL produce a schema-only change summary when refreshed metadata differs from the previous cached snapshot.

#### Scenario: 字段新增删除和属性变化
- **WHEN** refreshed metadata adds a column, removes a column, or changes data type, nullable flag, default value, comment, ordinal position, or index membership
- **THEN** DataSpec SHALL return a change summary with changed table names, added columns, removed columns, changed columns, totals, and bounded examples
- **AND** the summary MUST NOT include source database row values or credentials.

#### Scenario: 无变化刷新
- **WHEN** refreshed metadata matches the previous cached snapshot
- **THEN** DataSpec SHALL return an empty or zero-count change summary
- **AND** it SHALL still update `lastSeenAt` and source database product/version metadata.

### Requirement: AI 可读缓存证据
DataSpec SHALL expose metadata cache evidence in responses so AI can reason about freshness and rerun decisions.

#### Scenario: 返回 AI 可读缓存状态
- **WHEN** DataSpec returns database metadata cache information
- **THEN** it SHALL include `metadataFingerprint`, cache status, `lastSeenAt`, `expiresAt`, `refreshMode`, source database version, and safe next actions
- **AND** the AI-readable text MUST be sanitized before returning to clients.
