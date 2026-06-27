# dataspec-cli Specification

## Purpose
TBD - created by archiving change add-dataspec-cli. Update Purpose after archive.
## Requirements
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

### Requirement: Batch Lint SQL Files From CLI

The CLI SHALL provide a command that lints multiple SQL files for CI usage.

#### Scenario: Lint a directory

- **WHEN** a user runs `lint-files <dir> --project <id> --format json`
- **THEN** the CLI recursively collects `.sql` files from the directory
- **AND** it calls the configured DataSpec server `/api/lint` for each SQL file
- **AND** it prints a JSON object containing aggregate summary and per-file lint results

#### Scenario: Lint multiple input paths

- **WHEN** a user runs `lint-files <path-a> <path-b> --project <id> --format json`
- **THEN** the CLI scans each input path
- **AND** it de-duplicates discovered SQL files before linting

### Requirement: Batch Lint Exit Codes

The CLI SHALL use stable exit codes for batch linting.

#### Scenario: Any file has error issues

- **WHEN** any lint result has `errorCount` greater than zero
- **THEN** the CLI exits with code `1`

#### Scenario: No file has error issues

- **WHEN** every lint result has zero errors
- **THEN** the CLI exits with code `0`

#### Scenario: Batch lint request fails

- **WHEN** arguments are invalid or the server request fails
- **THEN** the CLI exits with code `2`
- **AND** it prints a readable error message to stderr

### Requirement: Pull Request SQL Review Comment

The CLI SHALL create or update a GitHub Pull Request comment containing DataSpec SQL lint results.

#### Scenario: Create review comment

- **WHEN** a user runs `review-pr <path...> --project <id> --repo <owner/name> --pr <number> --token <token>`
- **AND** the target pull request has no existing DataSpec review comment
- **THEN** the CLI batch lints the SQL files
- **AND** it creates a GitHub Pull Request comment containing summary, per-file counts, and issue details

#### Scenario: Update existing review comment

- **WHEN** the target pull request already has a comment containing the DataSpec review marker
- **THEN** the CLI updates that existing comment instead of creating a duplicate comment

### Requirement: Pull Request Review Exit Codes

The PR review command SHALL preserve CI-friendly exit codes after publishing feedback.

#### Scenario: Review has error issues

- **WHEN** any lint result has `errorCount` greater than zero
- **THEN** the CLI publishes or updates the PR comment
- **AND** it exits with code `1`

#### Scenario: Review has no error issues

- **WHEN** every lint result has zero errors
- **THEN** the CLI publishes or updates the PR comment
- **AND** it exits with code `0`

#### Scenario: GitHub request fails

- **WHEN** GitHub API comment creation or update fails
- **THEN** the CLI exits with code `2`
- **AND** it prints a readable error message to stderr
