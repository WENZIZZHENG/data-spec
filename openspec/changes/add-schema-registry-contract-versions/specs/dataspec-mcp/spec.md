## ADDED Requirements

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
