## ADDED Requirements

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
