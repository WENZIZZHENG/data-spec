## ADDED Requirements

### Requirement: Coverage report from schema dump
The system SHALL generate a field coverage report from a database schema dump without reconnecting to the source database.

#### Scenario: Report dump coverage
- **WHEN** a caller submits projectId and a valid schema dump to the coverage dump API
- **THEN** the system converts the dump to table definitions
- **AND** returns project-level summary, table summaries, field statuses, and unmanaged field rankings.

#### Scenario: Dump coverage excludes source data rows
- **WHEN** a coverage report is generated from dump input
- **THEN** the system analyzes only table and column metadata
- **AND** it does not require or expose source database row values.
