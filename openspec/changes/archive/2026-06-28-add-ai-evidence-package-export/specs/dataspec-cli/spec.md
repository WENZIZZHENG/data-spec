## ADDED Requirements

### Requirement: CLI evidence export command
The DataSpec CLI SHALL export AI evidence packages for supported sources.

#### Scenario: Export evidence JSON
- **WHEN** a user runs `evidence export --source-type <type> --source-id <id> --format json`
- **THEN** the CLI fetches the evidence package API
- **AND** prints stable JSON with `kind`, `schemaVersion`, `source`, `standardSnapshot`, `validationSummary`, `artifacts`, and `nextActions`.

#### Scenario: Export evidence zip
- **WHEN** a user runs `evidence export --source-type <type> --source-id <id> --format zip --output <path>`
- **THEN** the CLI downloads the zip evidence package to the requested path
- **AND** refuses to write unsafe paths outside the requested workspace target.

#### Scenario: Evidence command failure
- **WHEN** the evidence API returns a validation or not-found error
- **THEN** the CLI exits with code `2`
- **AND** prints a readable error without exposing token, password, Authorization header, or full JDBC URL.
