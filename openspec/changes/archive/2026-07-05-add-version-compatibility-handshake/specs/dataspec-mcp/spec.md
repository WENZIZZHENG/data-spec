## ADDED Requirements

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
