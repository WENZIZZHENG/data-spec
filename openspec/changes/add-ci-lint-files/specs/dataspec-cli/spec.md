## ADDED Requirements

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
