## ADDED Requirements

### Requirement: Session state fixture coverage
The DataSpec repository SHALL keep CLI/MCP contract fixtures for the MCP session state resource and tool.

#### Scenario: Fixture covers session state resource
- **WHEN** a developer runs `node tools/dataspec-cli-mcp-contract-check.mjs --format json`
- **THEN** it verifies a fixture entry for `dataspec://project/{projectId}/session-state`.
- **AND** descriptor drift in resource URI, name, description, or mime type is reported as a fixture diagnostic.

#### Scenario: Fixture covers session state tool
- **WHEN** the fixture check reads MCP `tools/list`
- **THEN** it verifies a fixture entry for `get_session_state`.
- **AND** the fixture preserves the input schema, read-only safety metadata, output shape, examples, and next actions for the tool.
