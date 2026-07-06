## ADDED Requirements

### Requirement: CLI fixedSql 文件补丁命令
DataSpec CLI SHALL expose a `fixed-sql patch` command that lets AI agents and users preview or apply fixed SQL to a local SQL file.

#### Scenario: CLI prints dry-run patch JSON
- **WHEN** a user runs `fixed-sql patch --lint-result <json> --target <file.sql> --format json`
- **THEN** the CLI reads the lint result and target file from the current workspace
- **AND** prints stable JSON containing `kind`, `schemaVersion`, `targetPath`, `dryRunResult`, `unifiedDiff`, `conflictWarnings`, `planHash`, `applyCommand`, `rollbackHint`, `evidenceRef`, `safety`, and `nextActions`
- **AND** exits with code `0` when a reviewable dry-run plan is produced.
- **AND** requires the lint result to include `fixedSql` plus `originalSql`, `sql`, or a matching original SQL hash.

#### Scenario: CLI applies with explicit confirmation
- **WHEN** a user runs `fixed-sql patch --lint-result <json> --target <file.sql> --apply --confirm <planHash> --format json`
- **THEN** the CLI re-computes the plan from current file content
- **AND** writes the fixed SQL only if the confirm hash matches
- **AND** prints stable JSON with `dryRunResult.status=APPLIED`
- **AND** exits with code `0`.

#### Scenario: CLI patch command blocks unsafe apply
- **WHEN** arguments are invalid, the lint-result or target path is outside the current workspace, a path is a symlink, fixed SQL is unavailable, original SQL evidence is unavailable, the file content has drifted, or confirm hash is missing or mismatched
- **THEN** the CLI exits with code `2`
- **AND** stderr contains a non-sensitive diagnostic and next action.

### Requirement: CLI fixedSql patch fixture coverage
The DataSpec repository SHALL keep contract fixture coverage for the `fixed-sql patch` command.

#### Scenario: Fixture covers fixedSql patch command
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies a fixture entry for `fixed-sql patch`
- **AND** the fixture documents required options, optional options, output shape, exit code semantics, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Fixture rejects unsafe examples
- **WHEN** the fixedSql patch fixture includes raw token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** the fixture check fails.
