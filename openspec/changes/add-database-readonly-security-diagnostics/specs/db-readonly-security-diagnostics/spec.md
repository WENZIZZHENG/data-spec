## ADDED Requirements

### Requirement: Database connection test returns read-only security diagnostics
DataSpec SHALL return a structured, sanitized security diagnostic when a database direct connection test succeeds.

#### Scenario: Read-only connection is marked safe
- **WHEN** a user tests a PostgreSQL or MySQL database connection that can be identified as read-only
- **THEN** the response keeps `success=true` and the existing readable success message
- **AND** the response includes `security.databaseType`, `security.currentUser`, `security.readOnly=true`, `security.writeRisk=false`, `security.riskLevel=SAFE`, accessible schema/table counts, warnings, recommended actions, and minimum-permission SQL guidance.

#### Scenario: Writable or privileged connection is marked dangerous
- **WHEN** a user tests a database connection that appears writable or has broad create/write privileges
- **THEN** the response keeps `success=true`
- **AND** `security.riskLevel` is `DANGER` or `WARNING`
- **AND** the response includes warnings and recommended actions that tell the user to prefer a read-only account for reverse import, compare, and coverage report workflows.

#### Scenario: Diagnostic uncertainty does not fail a usable connection
- **WHEN** the database connection succeeds but permission-specific metadata queries fail or the dialect is unsupported
- **THEN** the response keeps `success=true`
- **AND** the response includes `security.riskLevel=UNKNOWN` or a warning-level diagnostic explaining what could not be verified
- **AND** no write probe is executed against the source database.

### Requirement: Connection failure responses remain sanitized
DataSpec SHALL keep failed database connection responses readable while preventing secret leakage.

#### Scenario: Failed connection does not expose secrets
- **WHEN** a database connection test fails
- **THEN** the response keeps `success=false`
- **AND** the failure message MUST NOT include the submitted password, bearer token, full JDBC URL, or source database row data
- **AND** the response does not include misleading safe diagnostics.

### Requirement: Direct database frontend shows safety guidance
DataSpec Web SHALL display database connection security guidance in direct database workflows after a connection test.

#### Scenario: Reverse import shows diagnostic summary
- **WHEN** a user tests a database connection on the reverse import page
- **THEN** the page displays the diagnostic risk level, current user, read-only/write-risk status, accessible scope, warnings, recommended actions, and minimum-permission SQL when provided
- **AND** the existing table loading, preview, compare, and confirm import actions remain available.

#### Scenario: Coverage report shows diagnostic summary
- **WHEN** a user tests a database connection on the coverage report page
- **THEN** the page displays the same diagnostic summary
- **AND** the existing table loading and coverage report generation actions remain available.

#### Scenario: Frontend does not persist or reveal secrets
- **WHEN** a user tests a connection or views diagnostics
- **THEN** the page MUST NOT display or persist password, token, full JDBC URL, or source database row data
- **AND** only non-sensitive diagnostic fields are shown.

### Requirement: Minimum permission guidance is dialect aware
DataSpec SHALL provide dialect-aware minimum permission guidance for supported database types.

#### Scenario: PostgreSQL guidance is returned
- **WHEN** a PostgreSQL connection test succeeds
- **THEN** the diagnostic includes recommended SQL or actions for a read-only account scoped to metadata/table read access
- **AND** the guidance avoids destructive or write-oriented SQL.

#### Scenario: MySQL guidance is returned
- **WHEN** a MySQL connection test succeeds
- **THEN** the diagnostic includes recommended SQL or actions for a read-only account scoped to metadata/table read access
- **AND** the guidance avoids destructive or write-oriented SQL.
