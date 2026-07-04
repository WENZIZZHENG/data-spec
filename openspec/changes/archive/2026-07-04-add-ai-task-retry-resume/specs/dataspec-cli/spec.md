## ADDED Requirements

### Requirement: CLI task run commands
The DataSpec CLI SHALL expose AI task run status for local agents.

#### Scenario: List task runs from CLI
- **WHEN** a user runs `dataspec task list --project <id> --format json`
- **THEN** the CLI fetches project task runs and prints stable JSON with status, taskType, retryable, failedStep, resumeCommand, and source reference.

#### Scenario: List recent failed task runs from CLI
- **WHEN** a user runs `dataspec task failures --project <id> --format json`
- **THEN** the CLI prints recent failed or partially failed task runs with AI-readable next actions.

#### Scenario: Show task run detail from CLI
- **WHEN** a user runs `dataspec task show <id> --project <id> --format json`
- **THEN** the CLI prints parsed step statuses, partial artifacts, metadata, and resume command.

#### Scenario: Task command failure
- **WHEN** the task run API is unavailable or rejects access
- **THEN** the CLI exits with code `2` and prints an existing DataSpecError diagnostic without exposing token, password, Authorization header, or full JDBC URL.
