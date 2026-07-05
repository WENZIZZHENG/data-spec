## ADDED Requirements

### Requirement: MCP descriptors have contract fixture coverage
The DataSpec repository SHALL keep contract fixture entries for high-frequency MCP resources, prompts, and tools.

#### Scenario: Fixture covers MCP resources and prompts
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies fixture entries for high-frequency MCP resources such as `dataspec://version-compatibility`, `dataspec://capability-catalog`, `session-bootstrap`, `field-catalog`, `workflow-recipes`, `ai-task-profiles`, and `schema-registry`
- **AND** it verifies fixture entries for DataSpec MCP prompts used for create-table, SQL review, and field design guidance.

#### Scenario: Fixture covers MCP tools
- **WHEN** the fixture check reads MCP `tools/list`
- **THEN** it verifies fixture entries for high-frequency MCP tools such as `get_session_bootstrap`, `lint_sql`, `get_field_catalog`, `search_field_catalog`, `search_fields`, `suggest_fields`, `generate_table_ddl`, `get_ai_task_run`, and `export_evidence_package`
- **AND** each tool fixture preserves input schema shape, output shape, safety metadata, examples, and next actions.

### Requirement: MCP contract fixtures align with live descriptors
The DataSpec MCP server SHALL expose descriptors that can be checked against the local contract fixtures without calling backend APIs.

#### Scenario: MCP tool descriptor drifts
- **WHEN** an MCP tool name, input property, required input, or safety metadata changes without updating the fixture
- **THEN** the fixture check fails with a diagnostic naming the affected tool and contract path.

#### Scenario: MCP resource or prompt descriptor drifts
- **WHEN** an MCP resource URI, prompt name, description, or required argument changes without updating the fixture
- **THEN** the fixture check fails with a diagnostic naming the affected resource or prompt and contract path.
