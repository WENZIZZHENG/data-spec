## ADDED Requirements

### Requirement: CLI Synthetic Example Generation
The DataSpec CLI SHALL expose synthetic standard example generation as a machine-readable command for AI, CI, and fixture workflows.

#### Scenario: Generate synthetic examples as JSON
- **WHEN** a user runs `synthetic-examples generate --project <id> --scenario <user|order|payment|audit> --format json`
- **THEN** the CLI calls `/api/synthetic-examples/generate` on the configured DataSpec server
- **AND** it prints the returned synthetic example package JSON without removing stable fields.

#### Scenario: Generate synthetic examples as text summary
- **WHEN** a user runs `synthetic-examples generate --project <id> --scenario <scenario> --format text`
- **THEN** the CLI prints a concise summary containing scenario, `specHash`, generated case counts, safety summary, and next actions
- **AND** the text output does not become the stable machine-readable contract.

#### Scenario: Synthetic examples command failure
- **WHEN** arguments are invalid, the scenario is unsupported, or the server request fails
- **THEN** the CLI exits with code `2`
- **AND** it prints a readable diagnostic without exposing token, password, Authorization header, complete JDBC URL, DSN, or connection string values.

### Requirement: CLI synthetic examples fixture coverage
The DataSpec repository SHALL keep contract fixture coverage for the `synthetic-examples generate` command.

#### Scenario: Fixture covers synthetic examples command
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies a fixture entry for `synthetic-examples generate`
- **AND** the fixture documents required options, optional options, output shape, exit code semantics, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Fixture rejects unsafe synthetic examples
- **WHEN** the synthetic examples fixture includes raw token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** the fixture check fails.
