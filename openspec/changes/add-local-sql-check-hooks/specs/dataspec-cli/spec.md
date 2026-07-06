## ADDED Requirements

### Requirement: CLI install-hook command
The DataSpec CLI SHALL expose an `install-hook` command for explicitly enabling local SQL standard checks.

#### Scenario: Install hook with JSON output
- **WHEN** a user runs `dataspec install-hook --hook pre-commit --format json`
- **THEN** the CLI SHALL install or refresh the DataSpec-managed pre-commit hook in the current git repository.
- **AND** it SHALL print stable JSON with `kind`, `schemaVersion`, `hook`, `writtenFiles`, `skippedFiles`, `diagnostics`, `safety`, and `nextActions`.
- **AND** it SHALL exit with code `0` when all requested managed artifacts are written or already up to date.

#### Scenario: Install hook with VS Code files
- **WHEN** a user runs `dataspec install-hook --with-vscode --format json`
- **THEN** the CLI SHALL generate DataSpec-managed VS Code task and Problem Matcher files in `.vscode/`.
- **AND** the JSON output SHALL list those files and the local command each artifact uses.

#### Scenario: Install hook refuses unsafe overwrite
- **WHEN** the target hook or VS Code file exists without the DataSpec managed marker
- **THEN** the CLI SHALL skip that file, exit with code `2`, and print a non-sensitive diagnostic.
- **AND** it SHALL not partially overwrite unmanaged content.

#### Scenario: Install hook argument failure
- **WHEN** the user passes an unsupported hook name, unsupported format, positional argument, or runs outside a git repository
- **THEN** the CLI SHALL exit with code `2`.
- **AND** stderr SHALL contain a readable error that does not expose token, password, Authorization header, complete JDBC URL, DSN, or connection string.

### Requirement: CLI lint-changed text output
The DataSpec CLI SHALL support a text output mode for `lint-changed` that can be consumed by IDE Problem Matchers.

#### Scenario: Print matchable SQL issue lines
- **WHEN** a user runs `dataspec lint-changed --format text`
- **THEN** the CLI SHALL lint only changed SQL files using the existing changed-file workflow.
- **AND** it SHALL print one line per SQL issue using `file:line:column: severity rule - message` plus ` suggestion: <text>` when available.
- **AND** it SHALL preserve existing `lint-changed` exit code semantics.

#### Scenario: Print recoverable text diagnostics
- **WHEN** there are no changed SQL files or the changed-file workflow returns a recoverable diagnostic
- **THEN** the CLI SHALL print a concise non-sensitive text diagnostic and next actions.
- **AND** it SHALL not call the DataSpec server when no changed SQL files exist.
