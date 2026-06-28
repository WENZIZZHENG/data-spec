## ADDED Requirements

### Requirement: Expose machine-readable AI capability catalog
DataSpec SHALL expose a read-only capability catalog that describes stable AI-usable tasks and their invocation surfaces.

#### Scenario: List capabilities
- **WHEN** a caller requests the capability catalog
- **THEN** DataSpec returns schemaVersion, catalogVersion, generatedAt, capabilities, recommendedFirstActions, and diagnostics
- **AND** each capability has a stable id, category, title, summary, stability, status, project requirement, write risk, surfaces, input summary, output contracts, examples, preflight checks, docsRef, and nextActions.

#### Scenario: Show project-aware catalog
- **WHEN** a caller requests the catalog with projectId
- **THEN** DataSpec includes project-scoped diagnostics and recommendations
- **AND** no project assets or business data are modified.

#### Scenario: Catalog avoids unstable internals
- **WHEN** DataSpec includes a capability in the catalog
- **THEN** the capability only references supported public API endpoints, CLI commands, MCP resources/tools, frontend routes, workflow ids, profile ids, or contract ids
- **AND** experimental internal implementation details are omitted.

### Requirement: Retrieve a single capability by id
DataSpec SHALL allow callers to retrieve one capability entry by stable id.

#### Scenario: Known capability id
- **WHEN** a caller requests an existing capability id
- **THEN** DataSpec returns that capability entry with the same shape used in the list catalog.

#### Scenario: Unknown capability id
- **WHEN** a caller requests an unknown capability id
- **THEN** DataSpec returns a structured validation error with a suggested action to list available capabilities.

### Requirement: Capability catalog is safe by default
The capability catalog SHALL be descriptive and MUST NOT execute tasks or expose secrets.

#### Scenario: Read-only catalog request
- **WHEN** a caller reads the catalog
- **THEN** DataSpec does not run lint, export context, connect to databases, create fields, create evidence packages, or mutate project state
- **AND** the response does not include API tokens, passwords, Authorization headers, complete JDBC URLs, or source database rows.

#### Scenario: Write-capable tasks are described
- **WHEN** a capability can write DataSpec state or external comments
- **THEN** the catalog marks its writeRisk and required safeguards
- **AND** the catalog points to preflight checks or confirmation steps instead of executing the write.
