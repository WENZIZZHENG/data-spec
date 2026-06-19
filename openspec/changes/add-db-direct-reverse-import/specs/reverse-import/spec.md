## ADDED Requirements

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
