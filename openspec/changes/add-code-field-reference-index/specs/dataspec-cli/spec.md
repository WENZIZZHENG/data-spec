## ADDED Requirements

### Requirement: CLI field reference index command
The DataSpec CLI SHALL expose an `index-refs` command for local business-repository field reference indexing.

#### Scenario: Index references with JSON output
- **WHEN** a user runs `dataspec index-refs --field <name> --format json` from a repository with `.dataspec/config.json` `defaultPaths`
- **THEN** the CLI scans only the configured paths.
- **AND** it prints the code field reference index JSON without requiring a running DataSpec backend.
- **AND** it exits with code `0` when the scan completes.

#### Scenario: Index references with explicit paths
- **WHEN** a user runs `dataspec index-refs --field <name> --path <path> --format json`
- **THEN** the CLI scans the explicit path or paths instead of requiring `defaultPaths`.
- **AND** output paths are relative to the DataSpec config root or current working directory.

#### Scenario: Multiple aliases
- **WHEN** a user passes multiple `--field` or `--alias` values
- **THEN** the CLI searches all provided names as match candidates.
- **AND** each reference identifies the canonical requested `fieldName` it matched.
- **AND** when multiple `--field` values are present, alias values MUST use `field=alias` syntax so each alias has an unambiguous canonical field.

#### Scenario: CLI argument or scan error
- **WHEN** arguments are invalid, paths are missing, or no scan paths are configured
- **THEN** the CLI exits with code `2`.
- **AND** stderr contains a non-sensitive diagnostic and next action.

### Requirement: CLI field reference fixture coverage
The DataSpec repository SHALL keep contract fixture coverage for the `index-refs` command.

#### Scenario: Fixture covers index-refs command
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies a fixture entry for `index-refs`.
- **AND** the fixture documents required options, optional options, output shape, exit code semantics, safety metadata, examples, and recommended next actions.

#### Scenario: Fixture rejects unsafe examples
- **WHEN** the `index-refs` fixture includes raw token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** the fixture check fails.
