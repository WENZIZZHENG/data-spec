## MODIFIED Requirements

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
