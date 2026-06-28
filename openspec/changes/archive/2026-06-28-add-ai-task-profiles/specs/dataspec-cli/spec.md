## ADDED Requirements

### Requirement: AI profile CLI commands
The DataSpec CLI SHALL expose AI task profiles as machine-readable commands.

#### Scenario: List profiles
- **WHEN** a user runs a profile list command with JSON output
- **THEN** the CLI prints profiles with stable fields for profile id, task type, context scope, fixed SQL policy, output format, and recommended commands.

#### Scenario: Show profile
- **WHEN** a user requests a specific profile or task type
- **THEN** the CLI prints that profile and diagnostics
- **AND** unknown profiles return exit code `2` with supported values.

### Requirement: Profile defaults for context export and lint
The DataSpec CLI SHALL allow profile defaults to drive high-frequency AI commands.

#### Scenario: Export context uses profile scope
- **WHEN** a user runs `export-context` with a configured profile and no explicit scope options
- **THEN** the CLI forwards the profile's context scope defaults to the AI Context package endpoint.

#### Scenario: Lint uses profile fixed SQL policy
- **WHEN** a user runs `lint` or `lint-files` with a configured profile and no explicit fixed SQL policy
- **THEN** the CLI sends the profile selection to `/api/lint`
- **AND** explicit command options keep precedence.
