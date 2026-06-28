## ADDED Requirements

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
