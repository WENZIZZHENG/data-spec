# db-metadata-dump Specification

## Purpose
Define the database metadata adapter and schema dump JSON contract used to reproduce reverse import, compare, and coverage analysis without reconnecting to the source database.
## Requirements
### Requirement: Database schema dump contract
DataSpec SHALL expose a database schema dump JSON contract that contains database metadata needed for standard analysis without source database row data.

#### Scenario: Dump selected database tables
- **WHEN** a user requests a schema dump from a supported database connection and selected table names
- **THEN** DataSpec reads table, column, and index metadata through a read-only metadata flow
- **AND** returns databaseType, databaseName, schemaName, generatedAt, selected tables, columns, indexes, data types, nullable flags, default values, comments, table types, and warnings when available.

#### Scenario: Dump excludes secrets and row data
- **WHEN** DataSpec returns a schema dump
- **THEN** the dump MUST NOT include passwords, API tokens, bearer tokens, full JDBC URLs, or source database row values
- **AND** the dump MUST be safe to store as a local fixture or pass to AI for schema-only analysis.

### Requirement: Metadata adapter normalization
DataSpec SHALL normalize PostgreSQL and MySQL JDBC metadata into the same schema dump structure.

#### Scenario: PostgreSQL metadata is normalized
- **WHEN** PostgreSQL metadata includes schema, table remarks, and column metadata
- **THEN** DataSpec maps it into the schema dump tables and columns with stable field names.

#### Scenario: MySQL metadata is normalized
- **WHEN** MySQL metadata uses databaseName as catalog and does not use schemaName filtering
- **THEN** DataSpec maps catalog/table/column metadata into the schema dump
- **AND** includes warnings for known schema or comment limitations when relevant.

### Requirement: Dump to analysis table definitions
DataSpec SHALL convert a schema dump into the same parsed table definition model used by SQL reverse import, compare, and coverage analysis.

#### Scenario: Convert dump to TableDef list
- **WHEN** a schema dump contains one or more tables with columns
- **THEN** DataSpec converts each table to a `TableDef` with table name, comment, column name, dataType, nullable, defaultValue, and comment.

#### Scenario: Invalid dump is rejected
- **WHEN** a schema dump is missing projectId, tables, or required column names
- **THEN** DataSpec rejects the request with a readable business validation error.

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

### Requirement: Schema dump has real database integration coverage
DataSpec SHALL verify schema dump behavior against real PostgreSQL and MySQL containers through the optional database integration profile.

#### Scenario: PostgreSQL dump preserves comments and indexes
- **WHEN** the PostgreSQL integration test exports a dump from selected fixture tables
- **THEN** the dump SHALL include table comments, column comments, data types, nullable flags, schema names, and index metadata returned by the real PostgreSQL JDBC driver.
- **AND** the dump MUST NOT include credentials, full JDBC URLs, DSNs, or source database row values.

#### Scenario: MySQL dump preserves catalog metadata
- **WHEN** the MySQL integration test exports a dump from selected fixture tables
- **THEN** the dump SHALL include catalog/table metadata, useful table and column comments when available, data types, nullable flags, and index metadata returned by the real MySQL JDBC driver
- **AND** it SHALL expose MySQL schemaName/catalog limitations through warnings or stable diagnostics.
