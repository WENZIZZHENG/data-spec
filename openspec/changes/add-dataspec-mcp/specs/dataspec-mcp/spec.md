## ADDED Requirements

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
