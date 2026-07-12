## ADDED Requirements

### Requirement: CLI runs Standard Query DSL
DataSpec CLI SHALL expose a read-only Standard Query DSL search entry point with stable JSON output.

#### Scenario: CLI searches with DSL JSON
- **WHEN** an AI agent runs a Standard Query DSL CLI command with projectId and a DSL JSON object or file
- **THEN** stdout contains the stable DSL result JSON contract
- **AND** the command exits 0 when the query succeeds and 2 for argument, validation, config, or API errors.

#### Scenario: CLI search-fields accepts DSL
- **WHEN** an AI agent runs `search-fields` with DSL input
- **THEN** the CLI sends the DSL to the DataSpec server and prints the same stable field search JSON shape as the API.

#### Scenario: CLI DSL errors are secret-safe
- **WHEN** CLI query input, local config, or backend diagnostics contain secret-like text
- **THEN** stdout and stderr SHALL NOT expose raw token, password, Authorization, JDBC URL, DSN, or connection string values.
