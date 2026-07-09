## ADDED Requirements

### Requirement: CLI COMMENT patch plan preview command
The DataSpec CLI SHALL expose a read-only `comment-plan preview` command for database COMMENT patch plan review.

#### Scenario: Preview COMMENT plan as JSON
- **WHEN** a user runs `dataspec comment-plan preview --project <id> --database-type <type> --table <name> --format json`
- **THEN** the CLI calls the configured DataSpec server comment patch plan endpoint
- **AND** prints the returned plan JSON without removing stable fields
- **AND** exits with code `0` when the request succeeds.

#### Scenario: Preview COMMENT plan as text
- **WHEN** a user runs `dataspec comment-plan preview --project <id> --format text`
- **THEN** the CLI prints a concise readable summary containing total changes, risk level, unsupported count, dry-run SQL presence, metadata fingerprint, and next actions
- **AND** the text output is not the stable machine-readable contract.

#### Scenario: COMMENT plan command failure
- **WHEN** arguments are invalid, no table is selected, the database type is unsupported, or the server request fails
- **THEN** the CLI exits with code `2`
- **AND** stdout/stderr MUST NOT expose token, password, Authorization header, complete JDBC URL, DSN, or connection string values.

### Requirement: CLI COMMENT plan fixture coverage
The DataSpec repository SHALL keep contract fixture coverage for the `comment-plan preview` command.

#### Scenario: Fixture covers COMMENT plan preview
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies a fixture entry for `comment-plan preview`
- **AND** the fixture documents required options, optional options, output shape, exit code semantics, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Fixture rejects unsafe COMMENT plan examples
- **WHEN** the comment plan fixture includes raw token, password, Authorization header, API key, complete JDBC URL, DSN, connection string, or sampled source row values
- **THEN** the fixture check fails.
