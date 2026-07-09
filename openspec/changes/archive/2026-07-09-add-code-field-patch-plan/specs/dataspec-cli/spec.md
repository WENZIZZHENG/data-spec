## ADDED Requirements

### Requirement: CLI code field patch plan command
The DataSpec CLI SHALL expose a local read-only `code-patch plan` command that generates a business code Patch Plan for standard field changes.

#### Scenario: Print rename patch plan JSON
- **WHEN** a user runs `dataspec code-patch plan --field <old> --to-field <new> --path <file|dir> --format json`
- **THEN** the CLI scans only explicit paths or configured `defaultPaths`.
- **AND** it prints stable JSON containing `kind=dataspec.code-field.patch-plan`, `schemaVersion`, `change`, `scanSummary`, `candidateEdits[]`, `manualSteps[]`, `riskLevel`, `dryRunResult`, `verificationCommands[]`, `rollbackHint`, `safety`, `diagnostics[]`, and `nextActions[]`.
- **AND** it exits with code `0` when a dry-run plan is produced.
- **AND** it does not require a running DataSpec backend.

#### Scenario: Print patch plan Markdown
- **WHEN** a user runs `dataspec code-patch plan --field <name> --to-field <new> --format markdown`
- **THEN** the CLI prints a human-readable Patch Plan summary with risk, candidate files, suggested edits, manual steps, verification commands, rollback hint, and safety notes.
- **AND** the Markdown output is safe to paste into AI or a code review.

#### Scenario: Type and enum changes
- **WHEN** a user passes `--from-type`, `--to-type`, or one or more `--enum-change <old=new>` options
- **THEN** the CLI includes those changes in the Patch Plan.
- **AND** it emits manual review steps when a deterministic code diff is unsafe.

#### Scenario: CLI patch plan argument or scan failure
- **WHEN** arguments are invalid, no change intent is provided, `--field` is missing, scan paths are missing, no scan paths are configured, or a scan path is outside the business repository root
- **THEN** the CLI exits with code `2`.
- **AND** stderr contains a non-sensitive diagnostic and suggested action.

### Requirement: CLI code patch fixture coverage
The DataSpec repository SHALL keep contract fixture coverage for the `code-patch plan` command.

#### Scenario: Fixture covers code patch plan command
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies a fixture entry for `code-patch plan`.
- **AND** the fixture documents required options, optional options, output shape, exit code semantics, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Fixture rejects unsafe code patch examples
- **WHEN** the `code-patch plan` fixture includes raw token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** the fixture check fails.
