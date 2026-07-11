## ADDED Requirements

### Requirement: CLI exposes table standards
The DataSpec CLI SHALL expose read-only commands for table structure standards so AI agents can inspect them before generating DDL.

#### Scenario: List table standards as JSON
- **WHEN** a user runs `dataspec table-standards list --project <id> --format json`
- **THEN** the CLI calls the configured DataSpec server table standards endpoint
- **AND** it prints stable JSON containing business object summaries, template summaries, relation counts, safety metadata, and next actions.

#### Scenario: Show table standards as JSON
- **WHEN** a user runs `dataspec table-standards show --project <id> --template <id> --format json` or `--business-object <key>`
- **THEN** the CLI prints the matching table standard detail without executing DDL or writing project state
- **AND** it exits with code `2` for invalid arguments or backend failures using a non-sensitive diagnostic.

#### Scenario: CLI table standards fixture coverage
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies fixture entries for table-standard CLI commands
- **AND** the fixtures document required options, output shape, exit codes, safety metadata, examples, and recommended next actions.

### Requirement: CLI preserves DDL structure summary
The CLI SHALL preserve table structure standard fields returned by DDL preview.

#### Scenario: Generate DDL prints structure summary
- **WHEN** a user runs `dataspec generate-ddl --project <id> --template <id> --table <name> --format json`
- **THEN** the JSON output preserves `structureSummary` and existing `ddl`, `lintResult`, `standardSnapshot`, and `dialectDiagnostics`
- **AND** text output includes a concise structure summary when such data is returned by the server.
