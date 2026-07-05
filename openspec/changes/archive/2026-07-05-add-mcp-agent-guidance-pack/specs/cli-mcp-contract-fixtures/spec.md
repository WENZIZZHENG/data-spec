## ADDED Requirements

### Requirement: MCP resource templates have contract fixture coverage
The DataSpec repository SHALL keep contract fixture entries for high-frequency MCP resource templates.

#### Scenario: Fixture covers MCP resource templates
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies fixture entries for required MCP resource templates including the agent guidance pack template.

#### Scenario: MCP resource template descriptor drifts
- **WHEN** an MCP resource template URI template, description, or mime type changes without updating the fixture
- **THEN** the fixture check fails with a diagnostic naming the affected resource template and contract path.

### Requirement: MCP agent prompts have contract fixture coverage
The DataSpec repository SHALL keep contract fixture entries for first-class MCP agent prompts.

#### Scenario: Fixture covers first-class prompts
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies fixture entries for `create_table_with_dataspec`, `review_sql_with_dataspec`, `reverse_import_standards`, and `answer_field_standard_question`.

#### Scenario: Agent prompt descriptor drifts
- **WHEN** a prompt name, description, required argument, output shape, safety metadata, or recommended next action changes without updating the fixture
- **THEN** the fixture check fails with a diagnostic naming the affected prompt and contract path.
