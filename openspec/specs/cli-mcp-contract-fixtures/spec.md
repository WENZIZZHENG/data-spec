# cli-mcp-contract-fixtures Specification

## Purpose
定义高频 AI 入口的 CLI/MCP 契约 fixture 和本地校验规则，确保命令、工具、资源、prompt、示例、输出形状和安全元数据保持可追踪兼容。
## Requirements
### Requirement: CLI/MCP contract fixtures
DataSpec SHALL provide machine-readable contract fixtures for high-frequency AI-facing CLI and MCP entrypoints.

#### Scenario: Fixture lists supported entrypoints
- **WHEN** an AI agent or developer reads the contract fixture
- **THEN** it includes `kind`, `schemaVersion`, `cliCommands[]`, `mcpTools[]`, `mcpResources[]`, and `mcpPrompts[]`
- **AND** each entry includes a stable id or name, description, input boundary, output shape, success example, failure example when applicable, safety metadata, and recommended next actions.

#### Scenario: Fixture remains additive-friendly
- **WHEN** a CLI command or MCP descriptor gains an optional field without changing documented semantics
- **THEN** the fixture check continues to pass unless a documented stable field, safety boundary, example, or required entrypoint is removed or renamed.

### Requirement: Local contract fixture check
DataSpec SHALL provide a local validation command for CLI/MCP contract fixtures.

#### Scenario: Developer runs fixture check
- **WHEN** a developer runs the CLI/MCP contract fixture check command
- **THEN** it validates fixture structure, required high-frequency entrypoints, example redaction, and MCP descriptor alignment without calling a real DataSpec server.

#### Scenario: Fixture check failure is readable
- **WHEN** a required command, tool, resource, prompt, input shape, output shape, safety field, or example is missing
- **THEN** the command exits non-zero and reports diagnostics that identify the missing contract path.

### Requirement: Contract fixtures avoid secrets
CLI/MCP contract fixtures SHALL be safe to commit, log, and pass to AI.

#### Scenario: Fixture examples contain sensitive-looking values
- **WHEN** a fixture success example, failure example, diagnostic, or recommended command contains raw token, password, Authorization header, complete JDBC URL, DSN, or connection string text
- **THEN** the fixture check fails and reports the offending fixture path.

#### Scenario: Fixture uses placeholders
- **WHEN** an example needs to describe authentication, server URL, database connection, or sensitive inputs
- **THEN** it uses placeholders or redacted markers instead of reusable secrets.

### Requirement: MCP resource templates have contract fixture coverage
The DataSpec repository SHALL keep contract fixture entries for high-frequency MCP resource templates.

#### Scenario: Fixture covers MCP resource templates
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies fixture entries for required MCP resource templates including the agent guidance pack template.

#### Scenario: MCP resource template descriptor drifts
- **WHEN** an MCP resource template URI template, description, or mime type changes without updating the fixture
- **THEN** the fixture check fails with a diagnostic naming the affected resource template and contract path.

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

### Requirement: MCP agent prompts have contract fixture coverage
The DataSpec repository SHALL keep contract fixture entries for first-class MCP agent prompts.

#### Scenario: Fixture covers first-class prompts
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies fixture entries for `create_table_with_dataspec`, `review_sql_with_dataspec`, `reverse_import_standards`, and `answer_field_standard_question`.

#### Scenario: Agent prompt descriptor drifts
- **WHEN** a prompt name, description, required argument, output shape, safety metadata, or recommended next action changes without updating the fixture
- **THEN** the fixture check fails with a diagnostic naming the affected prompt and contract path.

### Requirement: CLI fixture covers contract import preview
The CLI/MCP contract fixtures SHALL document the `contract-import preview` command as a read-only AI-facing workflow.

#### Scenario: Contract import fixture is validated
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** the fixture set includes `contract-import-preview`
- **AND** the entry lists required options, optional options, output shape, exit codes, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Contract import fixture safety metadata
- **WHEN** the fixture check reads the `contract-import-preview` entry
- **THEN** its safety metadata declares `readOnly=true`, `writesProject=false`, `containsRealBusinessRows=false`, `externalNetworkUsed=false`, `externalLlmUsed=false`, and no raw sensitive example values.

### Requirement: CLI fixture covers synthetic examples
The CLI/MCP contract fixtures SHALL document the `synthetic-examples generate` command as a read-only AI-facing workflow.

#### Scenario: Synthetic examples fixture is validated
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** the fixture set includes `synthetic-examples-generate`
- **AND** the entry lists required options, optional options, output shape, exit codes, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Synthetic examples fixture safety metadata
- **WHEN** the fixture check reads the `synthetic-examples-generate` entry
- **THEN** its safety metadata declares `readOnly=true`, `writesProject=false`, `containsRealBusinessRows=false`, `externalLlmUsed=false`, and no raw sensitive example values.

### Requirement: CLI install-hook fixture coverage
The DataSpec repository SHALL keep contract fixture coverage for the `install-hook` command.

#### Scenario: Fixture covers install-hook command
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it SHALL verify a fixture entry for `install-hook`.
- **AND** the fixture SHALL document required options, optional options, generated artifacts, output shape, exit code semantics, safety metadata, examples, and recommended next actions.

#### Scenario: Fixture rejects unsafe hook examples
- **WHEN** the `install-hook` fixture includes raw token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** the fixture check SHALL fail with a readable diagnostic.
