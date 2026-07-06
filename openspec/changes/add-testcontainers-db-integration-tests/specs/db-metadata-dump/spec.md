## ADDED Requirements

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
