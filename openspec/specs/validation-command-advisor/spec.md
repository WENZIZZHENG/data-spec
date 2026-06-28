# validation-command-advisor Specification

## Purpose
定义 DataSpec 本地验证建议工具的路径匹配规则、AI 可读输出契约和文档入口，使开发者与 AI agent 能在提交或归档前按变更范围选择合适的最小验证命令。

## Requirements
### Requirement: Path-based validation advice
DataSpec SHALL provide a local validation advisor that recommends verification commands from changed file paths.

#### Scenario: Recommend commands for matched paths
- **WHEN** the advisor receives paths under backend, frontend, CLI/MCP tools, OpenSpec, documentation, or local deployment files
- **THEN** it SHALL return validation commands relevant to those paths
- **AND** each command SHALL include a reason, category, working directory, and estimated duration.

#### Scenario: Always include whitespace diff check
- **WHEN** the advisor receives any non-empty path list
- **THEN** it SHALL include `git diff --check` as a base verification command.

#### Scenario: Deduplicate command recommendations
- **WHEN** multiple changed paths match the same validation command
- **THEN** the advisor SHALL return that command only once
- **AND** it SHALL preserve enough reason text for users and AI agents to understand why it was selected.

### Requirement: AI-readable validation advice output
DataSpec SHALL expose validation advice in stable text and JSON formats.

#### Scenario: JSON output
- **WHEN** the advisor runs with JSON format
- **THEN** it SHALL output `kind`, `schemaVersion`, `inputPaths`, `commands`, `summary`, and `nextActions`
- **AND** each command SHALL include stable `id`, `command`, `cwd`, `reason`, `estimatedSeconds`, and `category` fields.

#### Scenario: Text output
- **WHEN** the advisor runs with text format
- **THEN** it SHALL print a concise human-readable list of commands with reasons and next actions.

### Requirement: Validation advisor documentation
DataSpec SHALL document the validation advisor entrypoint for users and AI agents.

#### Scenario: README references advisor
- **WHEN** users read the validation section
- **THEN** README SHALL show how to ask for recommended commands before running the full validation suite.

#### Scenario: AGENTS fragment references advisor
- **WHEN** `dataspec init --with-agents` writes or refreshes the managed AGENTS fragment
- **THEN** the fragment SHALL mention the validation advisor command as the first step after changing DataSpec-related files.
