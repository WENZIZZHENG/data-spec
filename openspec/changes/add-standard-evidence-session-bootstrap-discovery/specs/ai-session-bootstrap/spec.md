## MODIFIED Requirements

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
