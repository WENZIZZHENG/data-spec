## ADDED Requirements

### Requirement: CLI quality gate check
The DataSpec CLI SHALL expose a quality gate check command for CI and AI agents.

#### Scenario: Gate passes from CLI
- **WHEN** a user runs `dataspec quality-gate check --project <id> --format json` and the server returns gate status `PASS`
- **THEN** the CLI prints the gate result JSON and exits with code `0`.

#### Scenario: Gate fails from CLI
- **WHEN** a user runs `dataspec quality-gate check --project <id> --format json` and the server returns gate status `FAIL`
- **THEN** the CLI prints failed checks and next actions as JSON and exits with code `1`.

#### Scenario: Gate command request fails
- **WHEN** the quality gate API is unavailable or rejects access
- **THEN** the CLI exits with code `2` and prints an existing DataSpecError diagnostic without exposing token, password, Authorization header, or full JDBC URL.
