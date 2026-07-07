## ADDED Requirements

### Requirement: CLI AI Context budget plan command
The DataSpec CLI SHALL expose a `context-budget plan` command for AI Context budget planning.

#### Scenario: Print budget plan JSON
- **WHEN** a user runs `dataspec context-budget plan --project <id> --token-budget <n> --format json`
- **THEN** the CLI calls the configured DataSpec server budget planner endpoint.
- **AND** it prints the returned budget plan JSON without downloading, caching, or writing AI Context files.
- **AND** it exits with code `0` when the request succeeds.

#### Scenario: Forward scope and task hints
- **WHEN** a user passes `--profile`, `--task-type`, `--scope`, `--query`, `--status`, `--limit`, `--target-table`, or `--target-file`
- **THEN** the CLI forwards those values to the budget planner endpoint.
- **AND** explicit CLI options take precedence over `.dataspec/config.json` profile defaults.

#### Scenario: Budget plan command failure
- **WHEN** arguments are invalid, tokenBudget is missing or non-positive, or the server request fails
- **THEN** the CLI exits with code `2`.
- **AND** stderr contains a non-sensitive diagnostic without exposing token, password, Authorization header, complete JDBC URL, DSN, or connection string.

### Requirement: CLI budget planner fixture coverage
The DataSpec repository SHALL keep contract fixture coverage for the `context-budget plan` command.

#### Scenario: Fixture covers context-budget plan
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies a fixture entry for `context-budget plan`.
- **AND** the fixture documents required options, optional options, output shape, exit code semantics, safety metadata, examples, and recommended next actions.

#### Scenario: Fixture rejects unsafe budget examples
- **WHEN** the budget planner fixture includes raw token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** the fixture check fails.
