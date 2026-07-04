## ADDED Requirements

### Requirement: Database connection test returns health and dialect capability profile
DataSpec SHALL return a structured connection health profile from the direct database connection test without storing source credentials or business data rows.

#### Scenario: Successful connection returns capability profile
- **WHEN** a PostgreSQL or MySQL database connection test succeeds
- **THEN** the response SHALL include `health.connectionStatus=CONNECTED`, `health.latencyMs`, database product, version, dialect, schema/comment/index support, readonly check summary, required privileges, warnings, and next actions
- **AND** existing `security` diagnostics SHALL remain available for read-only risk guidance.

#### Scenario: Failed connection returns classified health diagnostic
- **WHEN** a database connection test fails because of authentication, network, schema, permission, or unsupported dialect problems
- **THEN** the response SHALL keep `success=false`
- **AND** the response SHALL include `health.connectionStatus=FAILED`, a stable `health.failureCategory`, `retryable`, sanitized message, warnings, and next actions
- **AND** it SHALL NOT include password, bearer token, full JDBC URL, or source database row data.

#### Scenario: Unknown dialect returns unsupported capability profile
- **WHEN** the requested database type is not supported
- **THEN** the response SHALL include a health profile with unsupported dialect capability
- **AND** it SHALL guide the user or AI to choose PostgreSQL/MySQL or stop direct reverse import for that connection.

### Requirement: Frontend displays connection health and capability profile
DataSpec Web SHALL show connection health and dialect capability details in existing direct database workflows.

#### Scenario: Reverse import shows health profile
- **WHEN** a user tests a connection on the reverse import page
- **THEN** the page SHALL display connection status, failure category when present, latency, database product/version, schema/comment/index support, warnings, and next actions.

#### Scenario: Coverage report shows health profile
- **WHEN** a user tests a connection on the coverage report page
- **THEN** the page SHALL display the same health profile
- **AND** existing table loading and report generation actions SHALL remain unchanged.
