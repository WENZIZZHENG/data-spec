## ADDED Requirements

### Requirement: Lint SQL From CLI
The system SHALL provide a CLI command that lints SQL by calling the DataSpec server.

#### Scenario: Lint file with JSON output
- **WHEN** a user runs `lint <path> --project <id> --format json`
- **THEN** the CLI reads SQL from the file
- **AND** it calls the configured DataSpec server `/api/lint`
- **AND** it prints the returned lint result as JSON

#### Scenario: Lint stdin
- **WHEN** a user runs `lint - --project <id> --format json`
- **THEN** the CLI reads SQL from stdin
- **AND** it sends that SQL to `/api/lint`

### Requirement: Lint Exit Codes
The CLI SHALL return stable exit codes for CI and AI agents.

#### Scenario: Lint has error issues
- **WHEN** `/api/lint` returns `errorCount` greater than zero
- **THEN** the CLI exits with code `1`

#### Scenario: Lint has no error issues
- **WHEN** `/api/lint` returns zero errors
- **THEN** the CLI exits with code `0`

#### Scenario: CLI request fails
- **WHEN** arguments are invalid or the server request fails
- **THEN** the CLI exits with code `2`
- **AND** it prints a readable error message to stderr

### Requirement: Export AI Context From CLI
The system SHALL provide a CLI command that downloads the AI Context zip package.

#### Scenario: Export context package
- **WHEN** a user runs `export-context --project <id> --output <zip>`
- **THEN** the CLI downloads `/api/ai-context/package/download`
- **AND** it writes the zip bytes to the requested output path

### Requirement: Server Configuration
The CLI SHALL support configuring the DataSpec server URL.

#### Scenario: Use default server
- **WHEN** no `--server` option is provided
- **THEN** the CLI uses `http://localhost:8090`

#### Scenario: Override server
- **WHEN** `--server <url>` is provided
- **THEN** the CLI sends requests to that server base URL
