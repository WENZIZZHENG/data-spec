# reverse-import Specification

## Purpose
TBD - created by archiving change add-reverse-sql-import. Update Purpose after archive.
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
- **AND** returns a reverse import preview containing tables, field candidates, missing comments, non-standard fields, and summary counts

### Requirement: Confirm Reverse Import Candidates

DataSpec SHALL allow confirmed reverse import field candidates to be written to the current project field library.

#### Scenario: Import new candidates

- **WHEN** a user confirms a reverse import preview
- **THEN** DataSpec creates standard fields for candidates that do not already exist in the project
- **AND** skips candidates whose field name already exists
- **AND** returns imported and skipped counts

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
