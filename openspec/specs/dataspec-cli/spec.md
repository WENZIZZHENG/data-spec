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

### Requirement: Cache AI Context From CLI
The CLI SHALL allow `export-context` to write a repository-local AI Context cache in addition to downloading a zip file.

#### Scenario: Export context cache
- **WHEN** a user runs `export-context --project <id> --cache`
- **THEN** the CLI downloads the AI Context package from the configured DataSpec server
- **AND** writes the package contents and cache metadata to `.dataspec/context/` under the configured repository root
- **AND** exits with code `0`.

#### Scenario: Cache mode preserves existing zip output
- **WHEN** a user runs `export-context --project <id> --output <zip> --cache`
- **THEN** the CLI writes the zip bytes to the requested output path
- **AND** also refreshes `.dataspec/context/`.

#### Scenario: Cache mode accepts scoped context options
- **WHEN** a user passes scope, query, status, limit, snapshotId, or snapshotVersion with `--cache`
- **THEN** the CLI forwards those options to the AI Context package endpoint
- **AND** records the same options in cache metadata.

#### Scenario: Cache mode rejects unsafe archive paths
- **WHEN** the downloaded package contains an absolute path or a path escaping the cache directory
- **THEN** the CLI rejects the package
- **AND** it exits with code `2` without writing outside `.dataspec/context/`.

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

### Requirement: CLI schema contract commands
The DataSpec CLI SHALL expose schema registry commands for AI agents and local validation.

#### Scenario: List contracts
- **WHEN** a user runs `contract list --format json`
- **THEN** the CLI fetches `/api/contracts`
- **AND** prints the registry catalog with stable `kind`, `schemaVersion`, `registryVersion`, and `contracts[]` fields.

#### Scenario: Show contract
- **WHEN** a user runs `contract show <contractId> --format json`
- **THEN** the CLI fetches `/api/contracts/{contractId}`
- **AND** prints the contract detail including JSON Schema, stable fields, deprecated fields, and compatibility.

#### Scenario: Check registry invariants
- **WHEN** a user runs `contract check --format json`
- **THEN** the CLI verifies required core contract ids, schemaVersion values, stableFields arrays, and compatibility policy are present
- **AND** exits with code `0` when the registry is usable or `2` with diagnostics when it is not.
