# reverse-import Specification

## Purpose
定义 SQL 文本、SQL 文件和数据库直连来源的反向导入预览流程，在确认前只读解析表字段、候选标准、缺注释和非标准摘要。
## Requirements
### Requirement: SQL Reverse Import Preview

DataSpec SHALL provide a read-only SQL reverse import preview.

#### Scenario: Preview SQL schema

- **WHEN** a user submits SQL DDL for a project
- **THEN** DataSpec parses tables and columns
- **AND** returns field candidates, missing comments, non-standard fields, and summary counts
- **AND** does not write to the standard field library.

### Requirement: Reverse Import Page

DataSpec SHALL expose a reverse import page for SQL text or `.sql` file input.

#### Scenario: View preview result

- **WHEN** a user submits SQL from the reverse import page
- **THEN** the page displays parsed tables, field candidates, missing comments, and non-standard fields.

### Requirement: Database Connection Reverse Import
DataSpec SHALL support reverse import preview from a live database connection.

#### Scenario: Test database connection
- **WHEN** a user submits database connection settings
- **THEN** DataSpec validates the connection without storing the password
- **AND** returns a success or readable failure message

#### Scenario: List database tables
- **WHEN** a user submits valid database connection settings
- **THEN** DataSpec lists available tables for the requested schema/catalog

#### Scenario: Preview selected tables
- **WHEN** a user selects one or more tables
- **THEN** DataSpec reads table and column metadata
- **AND** returns a reverse import preview containing tables, field candidates, missing comments, non-standard fields, mapping decisions, and summary counts

#### Scenario: Preview explains field mapping
- **WHEN** a database column matches a standard field by name or alias
- **THEN** the preview mapping decisions include `EXISTING_MATCH`, matched field identity, match reason, and confidence
- **AND** the matched column is not returned as a new field candidate.

#### Scenario: Preview explains new candidate
- **WHEN** a database column does not match a standard field name or alias
- **THEN** the preview mapping decisions include `NEW_CANDIDATE`, match reason, and confidence
- **AND** the column remains available as a confirmable field candidate.

### Requirement: Confirm Reverse Import Candidates
DataSpec SHALL allow confirmed reverse import field candidates to be written to the current project field library.

#### Scenario: Import new candidates
- **WHEN** a user confirms a reverse import preview
- **THEN** DataSpec creates standard fields for candidates that do not already exist in the project
- **AND** skips candidates whose field name already exists
- **AND** returns imported and skipped counts

#### Scenario: Return mapping decisions after confirmation
- **WHEN** a user confirms selected candidates and leaves other preview candidates unselected
- **THEN** the confirmation result includes a batch ID and mapping decisions for imported, skipped existing, and ignored candidates
- **AND** each decision includes a readable reason and any user-provided confirm or ignore reason.

### Requirement: Database Reverse Import Frontend

The frontend SHALL provide a database connection mode in the reverse import page.

#### Scenario: Preview and import through UI

- **WHEN** a user opens the reverse import page
- **THEN** they can choose SQL DDL mode or database connection mode
- **AND** database connection mode allows testing the connection, selecting tables, previewing differences, and confirming import

### Requirement: Reuse database connection presets in reverse import
The reverse import page SHALL allow users to reuse project database connection presets without restoring credentials.

#### Scenario: Load preset into direct connection form
- **WHEN** a user selects a database connection preset on the reverse import page
- **THEN** the page fills databaseType, host, port, databaseName, schemaName, and tableNames from the preset
- **AND** the password field remains empty or unchanged by the preset.

#### Scenario: Save current direct connection as preset
- **WHEN** a user saves the current direct connection form as a preset
- **THEN** the page submits only non-sensitive fields to the preset API
- **AND** password, token, JDBC URL, and full connection string are not submitted.

#### Scenario: Continue existing direct import workflow
- **WHEN** a user loads a preset and then enters the required password
- **THEN** the existing test connection, table loading, metadata preview, compare, and confirm import actions continue to work through the existing reverse import APIs.

### Requirement: Reverse import dialect diagnostics
Reverse import preview SHALL include dialect diagnostics for SQL text and database connection modes.

#### Scenario: SQL reverse import includes inferred dialect
- **WHEN** a user previews SQL DDL for reverse import
- **THEN** the preview result includes diagnostics for the inferred SQL dialect
- **AND** parser compatibility warnings remain available alongside field candidates

#### Scenario: Database reverse import uses connection dialect
- **WHEN** a user previews metadata through a PostgreSQL or MySQL database connection
- **THEN** diagnostics identify the selected `databaseType`
- **AND** schema/catalog/comment metadata limitations are returned as structured diagnostics when relevant

### Requirement: Reverse import preview from schema dump
DataSpec SHALL support reverse import preview from a database schema dump without reconnecting to the source database.

#### Scenario: Preview dump tables
- **WHEN** a caller submits projectId and a valid schema dump to the dump preview API
- **THEN** DataSpec converts the dump to table definitions
- **AND** returns the same reverse import preview structure used by SQL and database direct preview.

#### Scenario: Dump preview remains read-only
- **WHEN** a caller previews a schema dump
- **THEN** DataSpec does not write to the source database or standard field library
- **AND** candidate import still requires the existing explicit confirmation flow.

### Requirement: Reverse import confirmation write guard
Reverse import confirmation SHALL use the project-scoped write guard when importing selected database candidates.

#### Scenario: Retry candidate import with same key
- **WHEN** a caller submits the same selected candidates with the same idempotency key
- **THEN** DataSpec returns the original import result without creating duplicate fields or duplicate source records.

#### Scenario: Concurrent candidate import
- **WHEN** another reverse import confirmation is already running for the same project
- **THEN** DataSpec returns a retryable conflict diagnostic instead of interleaving candidate writes.
