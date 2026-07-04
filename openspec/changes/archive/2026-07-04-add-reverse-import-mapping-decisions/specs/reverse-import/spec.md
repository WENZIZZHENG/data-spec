## MODIFIED Requirements

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
