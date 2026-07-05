# cli-mcp-contract-fixtures Specification

## Purpose
TBD - created by archiving change add-cli-mcp-contract-fixtures. Update Purpose after archive.
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
