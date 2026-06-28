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
