# sql-check-records Specification

## Purpose
Define how SQL lint results, fixed SQL candidates, replay metadata, and issue-level fix metadata are persisted and reviewed after a SQL check.
## Requirements
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

### Requirement: SQL check replay metadata
SQL check record detail SHALL include standard snapshot replay metadata when available.

#### Scenario: Detail includes replay metadata
- **WHEN** a client requests a SQL check record detail
- **THEN** the response includes `replay.recordedStandard`, `replay.currentStandard`, `replay.status`, and `replay.nextActions`
- **AND** existing `record` and `issues` fields remain compatible

#### Scenario: Current standard differs
- **WHEN** the record snapshot hash differs from the current standard hash
- **THEN** replay status identifies that the record used a historical standard
- **AND** next actions include exporting historical context before applying fixes

### Requirement: SQL check record fix metadata
SQL check records SHALL preserve issue-level fixed SQL policy metadata for later review.

#### Scenario: Persist issue fix metadata
- **WHEN** a lint result is saved as a SQL check record
- **THEN** the serialized issues include fixer risk, change type, status, and explanation fields when present
- **AND** existing record fields remain readable by older clients.

#### Scenario: Record detail shows fix metadata
- **WHEN** a client requests SQL check record detail
- **THEN** the returned issues include the saved fixed SQL policy metadata
- **AND** the record still includes original SQL, fixed SQL, issue counts, and replay metadata.

### Requirement: SQL lint page fix plan review
The SQL lint page SHALL expose the fixed SQL policy and planned changes for the current lint result.

#### Scenario: Show fix plan summary
- **WHEN** a lint result includes a fix plan
- **THEN** the page displays effective policy, dry-run status, applied count, skipped count, and next actions.

#### Scenario: Show change risk beside fixed SQL
- **WHEN** fixed SQL or fixed SQL diff is displayed
- **THEN** the page shows planned changes with corresponding rule code, risk level, before value, after value, and explanation.
