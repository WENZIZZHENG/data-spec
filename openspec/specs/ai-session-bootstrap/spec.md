# ai-session-bootstrap Specification

## Purpose
TBD - created by archiving change add-ai-session-bootstrap. Update Purpose after archive.
## Requirements
### Requirement: AI Session Bootstrap API
DataSpec SHALL expose a read-only AI session bootstrap package for a DataSpec project.

#### Scenario: Project bootstrap is ready
- **WHEN** a caller requests the session bootstrap with a valid `projectId`
- **THEN** DataSpec returns `kind`, `schemaVersion`, `generatedAt`, `status`, `projectId`, `server`, `authMode`, `specVersion`, `standardSnapshot`, `availableCapabilities`, `recommendedCommands`, `knownRisks`, `docsRefs`, `checks`, and `nextActions`
- **AND** the package tells the caller whether lint, AI Context export, database reverse import, DDL generation, and standard evidence lookup are available through API, CLI, or MCP surfaces.
- **AND** `availableCapabilities` includes `standard-evidence` as a read-only API-only capability for `GET /api/standard-evidence`.

#### Scenario: Missing project id
- **WHEN** a caller requests the session bootstrap without `projectId`
- **THEN** DataSpec returns a machine-readable package with `status` set to `BLOCKED`
- **AND** `nextActions` includes commands or instructions to select a project before project-scoped actions.

#### Scenario: Current standard is unversioned
- **WHEN** the project has no current versioned standard snapshot
- **THEN** the bootstrap package still returns successfully
- **AND** the standard check records a warning with a next action to create or refresh a standard snapshot.

### Requirement: Bootstrap Safety
The AI session bootstrap package SHALL be safe to read at the beginning of an AI session.

#### Scenario: Read-only bootstrap
- **WHEN** a caller requests the bootstrap package
- **THEN** DataSpec does not run lint, export AI Context, connect to source databases, generate DDL, create records, or mutate project state.

#### Scenario: Secret redaction
- **WHEN** the bootstrap package is returned through API, CLI, or MCP
- **THEN** it does not include API token values, Authorization headers, database passwords, complete JDBC URLs, or source database rows
- **AND** it may only include coarse auth information such as `TOKEN_PRESENT`, `TOKEN_MISSING`, or `SERVER_SECURITY`.

### Requirement: Bootstrap Next Actions
DataSpec SHALL include structured next actions that AI agents can follow without guessing.

#### Scenario: Ready project
- **WHEN** the bootstrap package status is `READY`
- **THEN** `recommendedCommands` includes concrete CLI commands for doctor, lint, export-context, reverse-import workflow, and DDL generation guidance using the current project id.

#### Scenario: Blocked or degraded project
- **WHEN** a required check fails or warning exists
- **THEN** `nextActions` includes the check code, severity, message, command, docsRef, and whether the caller can retry after user action.
