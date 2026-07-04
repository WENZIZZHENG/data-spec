## ADDED Requirements

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
