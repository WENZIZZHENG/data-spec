## ADDED Requirements

### Requirement: MCP exposes table standards
The DataSpec MCP server SHALL expose table structure standards as read-only context for AI clients.

#### Scenario: List table standards resource
- **WHEN** an MCP client calls `resources/list`
- **THEN** the response includes a project table standards resource when a project id is configured
- **AND** the resource description identifies it as read-only business object, relation, template, and structure-standard context.

#### Scenario: Read table standards resource
- **WHEN** an MCP client reads the table standards resource
- **THEN** the MCP server fetches the DataSpec table standards API or AI Context table standards endpoint
- **AND** returns JSON text and `structuredContent` with business objects, templates, relations, summary, safety metadata, and next actions.

#### Scenario: Table standards tool
- **WHEN** an MCP client calls a read-only `get_table_standards` tool with optional project, template, or business object filters
- **THEN** the server returns the same table standards shape as JSON text and `structuredContent`
- **AND** it does not create templates, update standards, generate DDL, connect to databases, or mutate project state.

### Requirement: MCP create-table guidance uses table standards
MCP prompts for table creation SHALL guide AI clients to read table standards before DDL generation.

#### Scenario: Prompt resource sequence includes table standards
- **WHEN** an MCP client gets `create_table_with_dataspec` or a compatible table design prompt
- **THEN** the prompt guidance lists table standards before `generate_table_ddl`
- **AND** stop conditions mention missing or unsafe structure standards for high-risk DDL work.

#### Scenario: DDL tool preserves structure summary
- **WHEN** an MCP client calls `generate_table_ddl`
- **THEN** the structured response preserves the server `structureSummary` when present
- **AND** recommended next actions tell the client to inspect lint, dialect diagnostics, and structure evidence before handing DDL to a user.
