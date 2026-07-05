# dataspec-mcp Specification

## Purpose
TBD - created by archiving change add-dataspec-mcp. Update Purpose after archive.
## Requirements
### Requirement: MCP Initialization
The system SHALL provide a MCP stdio server that can be initialized by MCP clients.

#### Scenario: Initialize server
- **WHEN** a client sends an `initialize` JSON-RPC request
- **THEN** the server returns protocol version, server info, and capabilities for resources, prompts, and tools

### Requirement: MCP Resources
The MCP server SHALL expose DataSpec project resources for the configured project.

#### Scenario: List project resources
- **WHEN** a client calls `resources/list`
- **THEN** the server returns field catalog, database rules, and rules YAML resource descriptors

#### Scenario: Read field catalog
- **WHEN** a client calls `resources/read` for the field catalog URI
- **THEN** the server fetches `/api/ai-context/field-catalog`
- **AND** it returns the catalog as text content

### Requirement: MCP Prompts
The MCP server SHALL expose standard prompts for AI SQL workflows.

#### Scenario: List prompts
- **WHEN** a client calls `prompts/list`
- **THEN** the server returns prompts for creating tables, reviewing SQL, and designing fields from requirements

#### Scenario: Get prompt
- **WHEN** a client calls `prompts/get`
- **THEN** the server returns MCP prompt messages with DataSpec usage guidance

### Requirement: MCP Tools
The MCP server SHALL expose executable tools backed by the DataSpec server.

#### Scenario: Lint SQL tool
- **WHEN** a client calls `tools/call` with `lint_sql` and SQL text
- **THEN** the server calls `/api/lint`
- **AND** it returns the lint result as structured content and JSON text content

#### Scenario: Field catalog tool
- **WHEN** a client calls `tools/call` with `get_field_catalog`
- **THEN** the server calls `/api/ai-context/field-catalog`
- **AND** it returns the field catalog as structured content and JSON text content when possible

### Requirement: MCP Error Handling
The MCP server SHALL return stable JSON-RPC errors for invalid requests and failed backend calls.

#### Scenario: Unknown method
- **WHEN** a client calls an unsupported method
- **THEN** the server returns JSON-RPC error code `-32601`

#### Scenario: Backend request fails
- **WHEN** the DataSpec server returns a non-success response
- **THEN** the MCP server returns a JSON-RPC error with a readable message

### Requirement: MCP AI profile resource
The MCP server SHALL expose the configured DataSpec AI task profiles as resources for AI clients.

#### Scenario: List profile resources
- **WHEN** an MCP client calls `resources/list`
- **THEN** the response includes an AI task profiles resource for the configured project.

#### Scenario: Read profile resource
- **WHEN** an MCP client reads the AI task profiles resource
- **THEN** the server returns JSON text containing profiles, selected defaults, diagnostics, and recommended commands.

### Requirement: MCP profile guidance
The MCP server SHALL include AI task profile guidance in prompts.

#### Scenario: Prompt includes profile instruction
- **WHEN** an MCP client gets a workflow prompt
- **THEN** the prompt tells the agent to read the profile resource before choosing context scope, fixed SQL mode, or output format.

#### Scenario: Tool accepts profile hint
- **WHEN** an MCP lint or context tool receives a profile hint
- **THEN** it forwards the profile selection or derived defaults to the DataSpec API while preserving explicit tool arguments.

### Requirement: MCP schema registry resource
The MCP server SHALL expose the DataSpec schema registry as a resource for AI clients.

#### Scenario: List schema registry resource
- **WHEN** an MCP client calls `resources/list`
- **THEN** the response includes a `schema-registry` resource for the configured project.

#### Scenario: Read schema registry resource
- **WHEN** an MCP client reads the schema registry resource
- **THEN** the server returns JSON text containing registry metadata, contract summaries, JSON Schema refs, stable fields, deprecated fields, and compatibility policy.

#### Scenario: Prompts mention contract registry
- **WHEN** an MCP client gets a SQL or field design prompt
- **THEN** the prompt tells the agent to read the schema registry when it needs stable output field names or compatibility guidance.

### Requirement: MCP evidence package tool
The MCP server SHALL expose a tool for AI clients to retrieve evidence packages.

#### Scenario: Call evidence tool
- **WHEN** an MCP client calls `export_evidence_package` with a supported source type and id or payload
- **THEN** the server calls the DataSpec evidence package API
- **AND** returns the package as `structuredContent` and JSON text content.

#### Scenario: Evidence tool redaction
- **WHEN** the DataSpec API returns an evidence package
- **THEN** the MCP response preserves redacted fields and does not add token, password, Authorization header, or full JDBC URL to the content.

### Requirement: MCP evidence guidance
The MCP server SHALL guide AI clients to use evidence packages for handoff.

#### Scenario: Prompts mention evidence package
- **WHEN** an MCP client gets a SQL, field design, or workflow prompt
- **THEN** the prompt tells the agent to export an evidence package before handing off completed work.

### Requirement: MCP exposes capability catalog resource
The DataSpec MCP server SHALL expose the AI capability catalog as a resource that agents can read before choosing tools.

#### Scenario: Read global capability catalog resource
- **WHEN** an MCP client reads the capability catalog resource
- **THEN** the server returns JSON text and structuredContent containing the DataSpec capability catalog
- **AND** the resource metadata describes it as read-only self-description.

#### Scenario: Read project-scoped capability catalog resource
- **WHEN** the MCP server has a configured projectId
- **THEN** the capability catalog resource includes project-aware diagnostics from the DataSpec server.

#### Scenario: MCP catalog read failure
- **WHEN** the DataSpec server is unavailable or returns an error
- **THEN** the MCP response includes AI-readable DataSpec error data
- **AND** it does not fabricate a successful catalog.

### Requirement: MCP task run resource
The DataSpec MCP server SHALL expose recent AI task runs as a resource for configured projects.

#### Scenario: List task run resource
- **WHEN** an MCP client calls `resources/list`
- **THEN** the response includes an AI task runs resource when a project id is configured.

#### Scenario: Read task run resource
- **WHEN** an MCP client reads the AI task runs resource
- **THEN** the server returns JSON text and structured content containing recent failed and resumable task runs.

### Requirement: MCP task run tool
The DataSpec MCP server SHALL expose a read-only tool for AI task run detail.

#### Scenario: Get task run detail
- **WHEN** an MCP client calls `get_ai_task_run` with task run id and project id
- **THEN** the server returns task run detail as structuredContent and JSON text content.

#### Scenario: Task run tool failure
- **WHEN** the backend task run API fails
- **THEN** the MCP response includes AI-readable error data and does not fabricate a successful task state.

### Requirement: MCP session bootstrap resource
The DataSpec MCP server SHALL expose a session bootstrap resource for configured projects.

#### Scenario: List bootstrap resource
- **WHEN** an MCP client calls `resources/list`
- **THEN** the response includes `dataspec://project/<id>/session-bootstrap` with JSON mime type.

#### Scenario: Read bootstrap resource
- **WHEN** an MCP client reads the session bootstrap resource
- **THEN** the server fetches `/api/bootstrap/session`
- **AND** returns JSON text and `structuredContent` containing the bootstrap package.

#### Scenario: Bootstrap resource failure
- **WHEN** the DataSpec backend rejects or fails the bootstrap request
- **THEN** the MCP response includes AI-readable DataSpec error data
- **AND** it does not fabricate a successful bootstrap package.

### Requirement: MCP session bootstrap tool
The DataSpec MCP server SHALL expose a read-only `get_session_bootstrap` tool.

#### Scenario: Call bootstrap tool
- **WHEN** an MCP client calls `get_session_bootstrap` with an optional `projectId`
- **THEN** the server calls `/api/bootstrap/session`
- **AND** returns the package as JSON text and `structuredContent`.

### Requirement: MCP Task Card Tools
The DataSpec MCP server SHALL expose local task card tools for AI clients.

#### Scenario: Create task card tool
- **WHEN** an MCP client calls `create_task_card` with workflow id, goal, optional project id, and non-sensitive inputs
- **THEN** the server returns task card JSON text and `structuredContent`.

#### Scenario: Render task card tool
- **WHEN** an MCP client calls `render_task_card` with a task card object
- **THEN** the server returns Markdown text without mutating project state.

#### Scenario: Task card tool validation
- **WHEN** an MCP client provides an unknown workflow id, invalid task card, or unsafe sensitive input
- **THEN** the server returns a JSON-RPC error with AI-readable diagnostic data.

### Requirement: MCP Task Card Safety
MCP task card tools SHALL be local planning helpers, not workflow executors.

#### Scenario: No workflow execution
- **WHEN** a client creates or renders a task card
- **THEN** the MCP server does not call DataSpec write APIs, run lint, connect to source databases, or generate DDL.

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

### Requirement: MCP exposes version compatibility resource
The DataSpec MCP server SHALL expose the version compatibility handshake as a read-only resource.

#### Scenario: List compatibility resource
- **WHEN** an MCP client calls `resources/list`
- **THEN** the response includes a `dataspec://version-compatibility` resource
- **AND** the resource description tells AI clients to read it before running version-sensitive tools.

#### Scenario: Read compatibility resource
- **WHEN** an MCP client reads `dataspec://version-compatibility`
- **THEN** the MCP server requests `/api/capabilities/version`
- **AND** it returns the compatibility payload as JSON text and `structuredContent`.

#### Scenario: Compatibility resource failure is AI-readable
- **WHEN** the DataSpec server cannot provide the compatibility payload
- **THEN** the MCP server returns a JSON-RPC error with a DataSpec diagnostic and next action
- **AND** the error does not expose tokens, passwords, Authorization headers, JDBC URLs, DSNs, or connection strings.
