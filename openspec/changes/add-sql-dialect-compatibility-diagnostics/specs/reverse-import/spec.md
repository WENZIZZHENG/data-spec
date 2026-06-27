## ADDED Requirements

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
