## ADDED Requirements

### Requirement: CLI standard test data package command
The DataSpec CLI SHALL expose a read-only `test-data generate` command for generating standard-driven test data packages.

#### Scenario: Generate test data package as JSON
- **WHEN** a user runs `dataspec test-data generate --project <id> --format json`
- **THEN** the CLI calls the configured DataSpec server test data package endpoint
- **AND** it prints the returned package JSON without removing stable fields
- **AND** it exits with code `0` when the request succeeds.

#### Scenario: Generate test data package as text
- **WHEN** a user runs `dataspec test-data generate --project <id> --format text`
- **THEN** the CLI prints a concise summary containing `specHash`, case counts, seed profile counts, coverage summary, safety summary, diagnostics, and next actions
- **AND** the text output is not the stable machine-readable contract.

#### Scenario: Test data command failure
- **WHEN** arguments are invalid, selectors are unsupported, bounds are exceeded, or the server request fails
- **THEN** the CLI exits with code `2`
- **AND** stdout and stderr do not expose token, password, Authorization header, API key, complete JDBC URL, DSN, connection string, private key, or source database row values.

### Requirement: CLI consumer compatibility check command
The DataSpec CLI SHALL expose a local read-only `consumer-compat check` command for consumer compatibility suite validation.

#### Scenario: Run compatibility check as JSON
- **WHEN** a user runs `dataspec consumer-compat check --format json`
- **THEN** the CLI runs the local compatibility suite without requiring a running DataSpec server
- **AND** it prints stable JSON containing suite status, summary, golden payload checks, breaking rule results, adapter results, diagnostics, and next actions.

#### Scenario: Compatibility check exit codes
- **WHEN** all required adapters are compatible
- **THEN** the CLI exits with code `0`
- **AND** when the suite detects breaking or invalid fixtures it exits with code `1`
- **AND** when arguments are invalid or the suite cannot be loaded it exits with code `2`.

#### Scenario: Compatibility check output is secret-safe
- **WHEN** fixtures, diagnostics, examples, or recommended commands contain token, password, Authorization header, API key, complete JDBC URL, DSN, connection string, or private key patterns
- **THEN** the CLI redacts runtime output where possible and reports unsafe fixture paths without exposing the raw value.
