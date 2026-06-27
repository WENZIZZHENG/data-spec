## ADDED Requirements

### Requirement: Fixed SQL Output
The system SHALL include a deterministic fixed SQL candidate in lint results when it can be safely generated.

#### Scenario: Return fixed SQL from lint
- **WHEN** a client submits SQL to lint
- **THEN** the response includes `fixedSql`
- **AND** `fixedSql` is null or empty when the system cannot safely rebuild SQL

### Requirement: SQL Check Records
The system SHALL persist SQL check records for later review.

#### Scenario: List records
- **WHEN** a client requests SQL check records for a project
- **THEN** the system returns a paginated list ordered by newest first
- **AND** each record includes issue counts, original SQL, fixed SQL, and creation time

#### Scenario: View record detail
- **WHEN** a client requests one record by id
- **THEN** the system returns the record and its structured lint issues

### Requirement: SQL Lint Page Review Workflow
The SQL lint page SHALL expose fixed SQL and recent check records.

#### Scenario: Show and copy fixed SQL
- **WHEN** lint result contains `fixedSql`
- **THEN** the page displays a read-only fixed SQL block
- **AND** the user can copy it to the clipboard

#### Scenario: Browse recent records
- **WHEN** the SQL lint page opens
- **THEN** it loads recent records for the current project
- **AND** the user can paginate and view record details
