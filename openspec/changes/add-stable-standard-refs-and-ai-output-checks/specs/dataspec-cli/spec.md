## ADDED Requirements

### Requirement: CLI resolves standard references
DataSpec CLI SHALL expose project-scoped standard reference resolution with stable JSON output.

#### Scenario: Resolve reference from CLI
- **WHEN** an AI agent runs `dataspec ref resolve` with projectId, ref type, and one or more references
- **THEN** stdout SHALL contain the same structured resolution results as the API
- **AND** the command SHALL be read-only.

### Requirement: CLI checks AI output
DataSpec CLI SHALL expose deterministic AI output post-checks for file and stdin input.

#### Scenario: Check output file
- **WHEN** an AI agent runs `dataspec ai-output check` with projectId, content type, and a file or stdin
- **THEN** stdout SHALL contain the stable post-check JSON contract
- **AND** the process SHALL return exit code 0 for PASS, 1 for WARN or FAIL, and 2 for argument, config, or API errors.

#### Scenario: CLI errors are secret-safe
- **WHEN** input, API diagnostics, or local config contain secret-like text
- **THEN** stdout and stderr SHALL NOT expose raw token, password, Authorization, JDBC URL, DSN, or connection string values.
