## ADDED Requirements

### Requirement: Generate field coverage report from parsed tables
The system SHALL generate a project-level field coverage report from parsed database table definitions without modifying the source database or the DataSpec standard library.

#### Scenario: Report contains project, table, and field coverage
- **WHEN** a caller submits parsed tables for a project with existing standard fields
- **THEN** the system returns a summary with table count, column count, covered count, unmanaged count, missing comment count, and coverage rate.
- **AND** the response includes table-level summaries and field-level coverage statuses.

#### Scenario: Empty input is rejected
- **WHEN** a caller submits no tables or an empty table list
- **THEN** the system rejects the request with a business validation error.

### Requirement: Classify field coverage status
The system SHALL classify each database column by deterministic standard coverage status.

#### Scenario: Standard field name match
- **WHEN** a database column name matches an enabled standard field name in the project
- **THEN** the field coverage status is `STANDARD_MATCH`.
- **AND** the field is counted as covered.

#### Scenario: Standard alias match
- **WHEN** a database column name matches an alias of an enabled standard field in the project
- **THEN** the field coverage status is `ALIAS_MATCH`.
- **AND** the response identifies the matched standard field.

#### Scenario: Missing comment on matched field
- **WHEN** a matched database column has no comment
- **THEN** the field coverage status is `MISSING_COMMENT`.
- **AND** the field is counted as covered and missing comment.

#### Scenario: Unmanaged field with similar standard candidate
- **WHEN** a database column does not match standard field names or aliases but has a semantic recommendation candidate
- **THEN** the field coverage status is `POSSIBLE_DUPLICATE`.
- **AND** the response includes a recommended standard field and reason.

#### Scenario: Unmanaged field without candidate
- **WHEN** a database column does not match standard field names, aliases, or semantic candidates
- **THEN** the field coverage status is `UNMANAGED`.
- **AND** the field is counted as unmanaged.

### Requirement: Database direct coverage report
The system SHALL allow users to generate a coverage report from database direct metadata using the existing database connection request shape.

#### Scenario: Selected database tables are reported
- **WHEN** a user provides a database connection request with selected table names
- **THEN** the system reads metadata for those tables and returns a field coverage report.
- **AND** the source database is not modified.

#### Scenario: No selected tables are rejected
- **WHEN** a user requests a database direct coverage report without selected table names
- **THEN** the system rejects the request with a business validation error.

### Requirement: Coverage report frontend flow
The system SHALL provide a frontend flow for viewing field coverage reports for the current project.

#### Scenario: Generate and filter report
- **WHEN** a user selects the current project, connects to a database, selects tables, and generates a report
- **THEN** the page displays project-level coverage, table summaries, field details, and unmanaged field ranking.
- **AND** the user can filter field details by table and coverage status.

#### Scenario: Navigate to standard maintenance
- **WHEN** a report contains unmanaged or possible duplicate fields
- **THEN** the page provides actions to navigate to reverse import or the field library with useful query context.
