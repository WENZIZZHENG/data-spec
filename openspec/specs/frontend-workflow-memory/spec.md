# frontend-workflow-memory Specification

## Purpose
TBD - created by archiving change enhance-frontend-workflow-memory. Update Purpose after archive.
## Requirements
### Requirement: Project-scoped reverse import memory
The system SHALL remember reverse import workflow state per project in browser-local storage.

#### Scenario: Restore non-sensitive database state
- **WHEN** a user returns to database reverse import for a project with saved memory
- **THEN** the page restores database type, host, port, database name, schema name, username, selected table names, table search keyword, compare filter, and active mode.

#### Scenario: Isolate memory by project
- **WHEN** the selected DataSpec project changes
- **THEN** the page restores only the memory associated with the new project and does not reuse another project's table selection.

### Requirement: Sensitive values are never persisted
The system MUST NOT persist database password, API token, full JDBC URL, or other credential-like values in reverse import workflow memory.

#### Scenario: Save database state
- **WHEN** the page saves database reverse import memory
- **THEN** persisted state excludes password, token, connection string, and JDBC URL fields even if they exist in the source object.

#### Scenario: Read damaged memory
- **WHEN** browser-local storage contains malformed or incompatible memory data
- **THEN** the page falls back to default state without breaking the reverse import flow.

### Requirement: Field library deep-link filtering
The system SHALL allow the field library page to initialize from a route query keyword.

#### Scenario: Jump from import result to field library
- **WHEN** a reverse import result contains imported fields and the user opens the field library from that result
- **THEN** the field library route includes a keyword for the first imported field and the field library filters the visible field list by that keyword.

#### Scenario: Clear query-driven filter
- **WHEN** the route keyword is removed or changed
- **THEN** the field library updates its visible filter without requiring a page reload.
