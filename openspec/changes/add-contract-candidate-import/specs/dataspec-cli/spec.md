## ADDED Requirements

### Requirement: CLI Contract Candidate Import Preview
The DataSpec CLI SHALL expose contract candidate import preview as a machine-readable read-only command for AI, CI, and local review workflows.

#### Scenario: Preview contract import as JSON
- **WHEN** a user runs `contract-import preview --project <id> --source-kind <openapi|json-schema|protobuf> --input <path> --format json`
- **THEN** the CLI reads the local contract file
- **AND** calls `/api/contract-import/preview` on the configured DataSpec server
- **AND** prints the returned contract candidate preview package JSON without removing stable fields.

#### Scenario: Preview contract import as text summary
- **WHEN** a user runs `contract-import preview --project <id> --source-kind <sourceKind> --input <path> --format text`
- **THEN** the CLI prints a concise summary containing source kind, contract hash, candidate count, diagnostics, safety summary, and next actions
- **AND** the text output does not become the stable machine-readable contract.

#### Scenario: Contract import command failure
- **WHEN** arguments are invalid, the input file is missing, the source kind is unsupported, or the server request fails
- **THEN** the CLI exits with code `2`
- **AND** it prints a readable diagnostic without exposing token, password, Authorization header, complete JDBC URL, DSN, or connection string values.

### Requirement: CLI contract import fixture coverage
The DataSpec repository SHALL keep contract fixture coverage for the `contract-import preview` command.

#### Scenario: Fixture covers contract import preview command
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies a fixture entry for `contract-import preview`
- **AND** the fixture documents required options, optional options, output shape, exit code semantics, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Fixture rejects unsafe contract import examples
- **WHEN** the contract import fixture includes raw token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** the fixture check fails.
