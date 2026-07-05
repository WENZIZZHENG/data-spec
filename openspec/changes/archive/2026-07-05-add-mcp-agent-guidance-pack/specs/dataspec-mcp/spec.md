## ADDED Requirements

### Requirement: MCP first-class agent prompts
The DataSpec MCP server SHALL expose first-class prompts for common DataSpec agent workflows.

#### Scenario: List agent prompts
- **WHEN** an MCP client calls `prompts/list`
- **THEN** the response includes prompts named `create_table_with_dataspec`, `review_sql_with_dataspec`, `reverse_import_standards`, and `answer_field_standard_question`.

#### Scenario: Get agent prompt
- **WHEN** an MCP client calls `prompts/get` for a first-class agent prompt
- **THEN** the returned prompt text includes required inputs, safe defaults, recommended resource sequence, recommended tool sequence, stop conditions, and evidence requirements.

#### Scenario: Agent prompts preserve compatibility
- **WHEN** an MCP client calls existing prompt names `dataspec_create_table`, `dataspec_review_sql`, or `dataspec_design_fields`
- **THEN** those prompts remain available and continue to guide clients to DataSpec resources, safety metadata, and evidence package handoff.

### Requirement: MCP resource templates listing
The DataSpec MCP server SHALL support listing project-scoped resource templates.

#### Scenario: Resource templates list
- **WHEN** an MCP client calls `resources/templates/list`
- **THEN** the server returns `resourceTemplates[]` entries with `uriTemplate`, `name`, `description`, and `mimeType`.

#### Scenario: Projectless template discovery
- **WHEN** the MCP server starts without a configured project id
- **THEN** `resources/templates/list` still returns URI templates containing `{projectId}` placeholders rather than failing.
