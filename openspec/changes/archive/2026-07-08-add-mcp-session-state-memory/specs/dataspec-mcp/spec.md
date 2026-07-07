## ADDED Requirements

### Requirement: MCP session state resource
The DataSpec MCP server SHALL expose a read-only session state resource for configured projects.

#### Scenario: List session state resource
- **WHEN** an MCP client calls `resources/list` and the MCP server has a configured project id
- **THEN** the response includes `dataspec://project/<id>/session-state` with JSON mime type.
- **AND** the resource description identifies it as a local, read-only current project memory summary.

#### Scenario: Read session state resource
- **WHEN** an MCP client reads `dataspec://project/<id>/session-state`
- **THEN** the server returns JSON text and `structuredContent` containing `kind`, `schemaVersion`, `generatedAt`, `currentProject`, `currentSnapshot`, `lastTaskResult`, `toolCursor`, `safeDefaults`, `redactedMemory`, `diagnostics`, and `nextActions`.
- **AND** the server does not call DataSpec write APIs, run lint, connect to source databases, generate DDL, or mutate project state.

#### Scenario: Projectless template discovery
- **WHEN** the MCP server starts without a configured project id
- **THEN** `resources/templates/list` includes `dataspec://project/{projectId}/session-state`.

### Requirement: MCP session state tool
The DataSpec MCP server SHALL expose a read-only `get_session_state` tool.

#### Scenario: Call session state tool
- **WHEN** an MCP client calls `get_session_state` with an optional `projectId`
- **THEN** the server returns the same session state shape as JSON text and `structuredContent`.
- **AND** an explicit `projectId` overrides the configured project id for the returned state.

#### Scenario: Missing project id
- **WHEN** an MCP client calls `get_session_state` without a project id and no project id is configured
- **THEN** the server returns a valid session state with `currentProject.status` set to `BLOCKED`.
- **AND** `nextActions` tells the client to provide `projectId`, update `.dataspec/config.json`, or call `get_session_bootstrap` after selecting a project.

### Requirement: MCP session state safety
The MCP session state SHALL be safe for AI clients to read at the beginning and middle of a session.

#### Scenario: Session state redaction
- **WHEN** local config, context metadata, task cards, commands, server URLs, or diagnostics contain tokens, passwords, Authorization headers, JDBC URLs, DSNs, connection strings, or URL userinfo
- **THEN** `resources/read`, `tools/call`, JSON text, and `structuredContent` do not reveal raw secret-like values.
- **AND** the output may only include presence flags such as `TOKEN_PRESENT`, counts, hashes, or redacted placeholders.

#### Scenario: Session state is not authorization
- **WHEN** the session state says a project id or token is configured
- **THEN** `safeDefaults` and `diagnostics` state that the snapshot is not an authorization decision.
- **AND** write-capable follow-up tools still require their existing safety metadata, user confirmation, dry-run, or idempotency checks.
