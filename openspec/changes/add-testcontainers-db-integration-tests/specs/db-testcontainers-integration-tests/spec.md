## ADDED Requirements

### Requirement: 可选真实数据库集成测试入口
DataSpec SHALL provide an opt-in backend integration test profile that runs real PostgreSQL and MySQL metadata workflows with Testcontainers without changing the default unit test command.

#### Scenario: Default backend tests do not require Docker
- **WHEN** a developer runs the normal backend unit test command
- **THEN** Docker containers SHALL NOT be required for the command to start
- **AND** Testcontainers integration tests SHALL only run when the dedicated database integration profile is explicitly enabled.

#### Scenario: Dedicated profile runs container tests
- **WHEN** a developer runs the dedicated database integration Maven profile
- **THEN** DataSpec SHALL start PostgreSQL and MySQL containers for the integration test matrix
- **AND** the command SHALL fail with a dialect-specific test failure when metadata, compare, coverage, or connection diagnostics drift.

### Requirement: 集成测试 fixture 保持 schema-only
DataSpec SHALL keep database integration fixtures limited to schema metadata needed for standard analysis.

#### Scenario: Fixtures do not insert source rows
- **WHEN** Testcontainers schemas are initialized for PostgreSQL or MySQL
- **THEN** the fixtures SHALL create tables, comments, indexes, and least-privilege users needed for metadata tests
- **AND** they MUST NOT require source database business rows to validate reverse import, compare, or coverage behavior.

#### Scenario: Test artifacts do not expose credentials
- **WHEN** integration tests produce assertions, logs, README instructions, or OpenSpec evidence
- **THEN** those artifacts MUST NOT include raw database passwords, bearer tokens, full JDBC URLs, DSNs, or source database row values.

### Requirement: 真实数据库矩阵覆盖核心 metadata 流程
DataSpec SHALL verify the supported PostgreSQL/MySQL metadata paths against real JDBC drivers through the dedicated integration profile.

#### Scenario: PostgreSQL matrix validates schema metadata
- **WHEN** the PostgreSQL integration fixture contains schema-qualified tables, COMMENT metadata, and indexes
- **THEN** DataSpec SHALL list selected tables and export metadata with schema name, table comment, column comments, nullable/data type information, and index metadata.

#### Scenario: MySQL matrix validates catalog metadata
- **WHEN** the MySQL integration fixture contains commented tables, commented columns, and indexes
- **THEN** DataSpec SHALL treat databaseName as catalog, preserve useful comments when provided by the driver, return index metadata, and expose known schemaName limitations through warnings or stable diagnostics.
