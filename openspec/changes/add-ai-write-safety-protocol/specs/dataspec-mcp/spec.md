## ADDED Requirements

### Requirement: MCP exposes tool safety metadata
The DataSpec MCP server SHALL expose AI write safety metadata in resources or tool metadata so MCP clients can inspect risk before invoking a tool.

#### Scenario: Capability resource includes safety
- **WHEN** an MCP client reads the capability catalog resource
- **THEN** the returned JSON text and `structuredContent` preserve each capability `safety` object.

#### Scenario: Tool list includes local safety metadata
- **WHEN** an MCP client calls `tools/list`
- **THEN** each DataSpec tool descriptor includes safety metadata or a safety reference describing whether the tool is read-only, writes project state, requires dry-run, requires idempotency key, accepts sensitive inputs, and what next action to take.

#### Scenario: MCP prompts mention safety first
- **WHEN** an MCP client reads DataSpec prompts for SQL review, table creation, or field design
- **THEN** the prompt tells the AI to inspect capability safety metadata before invoking write-capable tools.

### Requirement: MCP returns structured safety diagnostics
The DataSpec MCP server SHALL propagate or create AI-readable JSON-RPC safety errors for missing required safety parameters.

#### Scenario: Backend safety error is preserved
- **WHEN** a DataSpec backend write API rejects a request with a safety diagnostic
- **THEN** the MCP JSON-RPC error includes `error.data.dataspecError` with the diagnostic code, category, missing fields, safety metadata, and next actions.

#### Scenario: Local safety validation fails
- **WHEN** an MCP tool declares a required safety parameter and the client omits it
- **THEN** the MCP server rejects the call before invoking the backend
- **AND** the JSON-RPC error describes the missing parameter and safe retry action.

#### Scenario: MCP safety output is redacted
- **WHEN** MCP tools/list, resources/read, tools/call, or errors include safety metadata
- **THEN** the output does not include raw token, password, Authorization header, API key, complete JDBC URL, DSN, connection string, or source database rows.
