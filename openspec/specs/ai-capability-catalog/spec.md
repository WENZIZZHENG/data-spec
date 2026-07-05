# ai-capability-catalog Specification

## Purpose
TBD - created by archiving change add-ai-capability-catalog. Update Purpose after archive.
## Requirements
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

### Requirement: Capability catalog includes session bootstrap
The AI capability catalog SHALL describe the session bootstrap entry point.

#### Scenario: List session bootstrap capability
- **WHEN** a caller lists the AI capability catalog
- **THEN** the catalog includes a stable `session-bootstrap` capability
- **AND** the capability lists API, CLI, and MCP surfaces for reading the AI session bootstrap package.

#### Scenario: Bootstrap capability safety
- **WHEN** the catalog describes `session-bootstrap`
- **THEN** it marks the capability as read-only
- **AND** its preflight checks and next actions explain that the bootstrap does not execute lint, export context, reverse import, DDL generation, or writes.

### Requirement: Capability entries expose write safety metadata
The AI capability catalog SHALL expose the unified AI write safety metadata for every capability entry.

#### Scenario: List capability safety
- **WHEN** a caller requests the capability catalog
- **THEN** each capability includes a `safety` object with `readOnly`, `writesProject`, `requiresDryRun`, `supportsUndo`, `requiresIdempotencyKey`, `sensitiveInputs`, and `nextActions`
- **AND** existing `writeRisk`, `preflightChecks`, and top-level `nextActions` fields remain available for compatible clients.

#### Scenario: Show capability safety
- **WHEN** a caller requests a single capability by id
- **THEN** the response includes the same `safety` object used in the list catalog
- **AND** the safety object does not execute the capability or grant extra permission.

#### Scenario: High-risk capability guidance
- **WHEN** a capability can perform high-risk project writes such as standard merge, reverse import confirmation, project restore apply, standard reuse apply, starter kit apply, or AI batch writes
- **THEN** the catalog marks `safety.requiresDryRun` or `safety.requiresIdempotencyKey` according to that operation's required safeguards
- **AND** `safety.nextActions` points to the dry-run, preview, idempotency, evidence, or recovery step.

#### Scenario: Catalog safety remains non-secret
- **WHEN** the catalog describes operations with sensitive inputs
- **THEN** `safety.sensitiveInputs` lists only safe parameter names or categories
- **AND** the catalog response does not include API tokens, passwords, Authorization headers, complete JDBC URLs, DSNs, or source database rows.

### Requirement: Capability catalog includes SQL rule debugger
The AI capability catalog SHALL describe the SQL rule debugger as a read-only capability for rule troubleshooting.

#### Scenario: List SQL rule debugger capability
- **WHEN** a caller lists the AI capability catalog
- **THEN** the catalog includes a stable `sql-rule-debugger` capability
- **AND** the capability lists the `/api/lint/debug` API surface, CLI `lint-debug` surface, output contract, preflight checks, and next actions.

#### Scenario: SQL rule debugger safety
- **WHEN** the catalog describes `sql-rule-debugger`
- **THEN** it marks the capability as read-only
- **AND** it explains that the capability does not save SQL check records, change rules, create suppressions, or mutate project state.

### Requirement: Capability catalog includes version compatibility handshake
The AI capability catalog SHALL describe the version compatibility handshake as a read-only preflight capability.

#### Scenario: List version compatibility capability
- **WHEN** a caller requests the capability catalog
- **THEN** the catalog includes a stable `version-compatibility` capability
- **AND** the capability lists `/api/capabilities/version`, CLI `compat check`, and MCP `dataspec://version-compatibility` surfaces.

#### Scenario: Recommended first actions mention compatibility
- **WHEN** the catalog returns `recommendedFirstActions`
- **THEN** at least one recommended action tells AI clients to check version compatibility before executing CLI or MCP workflows that depend on server capabilities.

#### Scenario: Version compatibility safety metadata
- **WHEN** the catalog describes `version-compatibility`
- **THEN** it marks the capability as read-only
- **AND** its safety metadata states that it does not write project state, connect to source databases, or expose secrets.

