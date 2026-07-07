## ADDED Requirements

### Requirement: CLI AI Context 质量检查命令
The DataSpec CLI SHALL expose a local read-only `context-quality check` command that evaluates whether an exported AI Context package or budget plan is sufficient for an AI task.

#### Scenario: Check cached context directory as JSON
- **WHEN** a user runs `dataspec context-quality check --context-dir <dir> --format json`
- **THEN** the CLI reads AI Context files from the directory without calling the DataSpec server.
- **AND** it prints stable JSON containing `kind`, `schemaVersion`, `input`, `contextQualityScore`, `qualityLevel`, `tokenBudgetBreakdown`, `missingCriticalResources`, `truncatedResources`, `coverageByCategory`, `taskFitHints`, and `nextContextActions`.
- **AND** it exits with code `0` when the directory can be evaluated.

#### Scenario: Check context zip as JSON
- **WHEN** a user runs `dataspec context-quality check --context-zip <zip> --format json`
- **THEN** the CLI reads the AI Context zip locally without extracting files outside the workspace.
- **AND** it reports unsafe archive entries as a command failure instead of evaluating them.

#### Scenario: Check budget plan as JSON
- **WHEN** a user runs `dataspec context-quality check --budget-plan <json> --format json`
- **THEN** the CLI reads a `context-budget plan` JSON file locally.
- **AND** it derives score, resource gaps, task fit hints, and next actions from the selected artifacts, dropped artifacts, token estimation, quality risk, fallback steps, and recommended next actions.

#### Scenario: Print human-readable quality summary
- **WHEN** a user runs `dataspec context-quality check` with `--format text`
- **THEN** the CLI prints a concise summary of score, level, missing critical resources, truncated resources, and next actions.
- **AND** the text output is not the stable machine-readable contract.

#### Scenario: Quality command failure
- **WHEN** arguments are invalid, no input source is provided, multiple input sources are provided, an input file is missing, a zip is unsafe, or JSON input cannot be parsed.
- **THEN** the CLI exits with code `2`.
- **AND** stderr contains a non-sensitive diagnostic without exposing token, password, Authorization header, complete JDBC URL, DSN, or connection string values.

### Requirement: CLI AI Context 质量检查 fixture 覆盖
The DataSpec repository SHALL keep contract fixture coverage for the `context-quality check` command.

#### Scenario: Fixture covers context-quality check
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies a fixture entry for `context-quality check`.
- **AND** the fixture documents required options, optional options, output shape, exit code semantics, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Fixture rejects unsafe quality examples
- **WHEN** the context quality fixture includes raw token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values.
- **THEN** the fixture check fails.
