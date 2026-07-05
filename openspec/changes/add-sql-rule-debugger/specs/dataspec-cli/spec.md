## ADDED Requirements

### Requirement: CLI SQL rule debug command
The DataSpec CLI SHALL expose a `lint-debug` command that returns SQL rule debug output from the server.

#### Scenario: Debug SQL file with JSON output
- **WHEN** a user runs `lint-debug <path> --project <id> --format json`
- **THEN** the CLI reads SQL from the file
- **AND** it calls the configured DataSpec server `/api/lint/debug`
- **AND** it prints the returned debug result as stable JSON.

#### Scenario: Debug SQL from stdin
- **WHEN** a user runs `lint-debug - --project <id> --format json`
- **THEN** the CLI reads SQL from stdin
- **AND** it sends that SQL to `/api/lint/debug`
- **AND** it exits with code `0` when the request succeeds, regardless of lint issue severity.

#### Scenario: Debug request fails
- **WHEN** `lint-debug` arguments are invalid or the server request fails
- **THEN** the CLI exits with code `2`
- **AND** it prints a non-sensitive readable error message to stderr.
