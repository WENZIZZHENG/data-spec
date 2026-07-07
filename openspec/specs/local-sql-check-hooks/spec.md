# local-sql-check-hooks Specification

## Purpose
定义 DataSpec 如何安装和管理本地 SQL 检查 hook，让用户在提交前以只读方式运行变更 SQL 校验，同时保护已有非 DataSpec 管理的 hook。
## Requirements
### Requirement: Managed local SQL check hook
DataSpec SHALL provide a local installation workflow for a DataSpec-managed pre-commit SQL check hook.

#### Scenario: Install managed pre-commit hook
- **GIVEN** the command runs inside a git repository
- **AND** the target hook is missing or already contains the DataSpec managed marker
- **WHEN** a user runs `dataspec install-hook --hook pre-commit --format json`
- **THEN** DataSpec SHALL write `.git/hooks/pre-commit` inside the current repository.
- **AND** the generated hook SHALL run `lint-changed --format json` before commit.
- **AND** the installation output SHALL list the written hook path, command, safety metadata, and next actions.

#### Scenario: Preserve unmanaged user hook
- **GIVEN** `.git/hooks/pre-commit` exists without the DataSpec managed marker
- **WHEN** a user runs `dataspec install-hook --hook pre-commit --format json`
- **THEN** DataSpec SHALL refuse to overwrite that hook.
- **AND** it SHALL return a non-sensitive `HOOK_EXISTS_UNMANAGED` diagnostic with manual merge guidance.

#### Scenario: No git repository
- **GIVEN** the command runs outside a git repository
- **WHEN** a user runs `dataspec install-hook --format json`
- **THEN** DataSpec SHALL not write files.
- **AND** it SHALL return a non-sensitive `NO_GIT_REPOSITORY` diagnostic with recovery guidance.

### Requirement: VS Code local SQL check task
DataSpec SHALL optionally generate VS Code task and Problem Matcher examples for local SQL standard checks.

#### Scenario: Generate VS Code task files
- **GIVEN** the command runs inside a git repository
- **WHEN** a user runs `dataspec install-hook --with-vscode --format json`
- **THEN** DataSpec SHALL write `.vscode/tasks.json` and `.vscode/dataspec-problem-matcher.json` when those files are missing or DataSpec-managed.
- **AND** the generated task SHALL run `lint-changed --format text`.
- **AND** the generated Problem Matcher SHALL parse file, line, column, severity, rule, message, and suggestion from the text output.

#### Scenario: Preserve unmanaged VS Code files
- **GIVEN** `.vscode/tasks.json` or `.vscode/dataspec-problem-matcher.json` exists without the DataSpec managed marker
- **WHEN** a user runs `dataspec install-hook --with-vscode --format json`
- **THEN** DataSpec SHALL skip the unmanaged file.
- **AND** the installation output SHALL include a diagnostic and manual merge next action instead of overwriting it.

### Requirement: Local SQL check output is safe for AI and IDEs
DataSpec SHALL provide local check outputs that are machine-readable, IDE-matchable, and safe to log.

#### Scenario: Pre-commit output remains AI readable
- **WHEN** the generated pre-commit hook runs and SQL lint fails
- **THEN** the hook SHALL expose the `lint-changed --format json` output.
- **AND** the output SHALL include changed files, per-file lint results, issue severity, rule, location, suggestion when available, diagnostics, and next actions.

#### Scenario: Text output is IDE matchable
- **WHEN** a user runs `dataspec lint-changed --format text`
- **THEN** each SQL issue SHALL be printed as a single line containing file, line, column, severity, rule, message, and optional suggestion.
- **AND** the command exit code SHALL keep existing `lint-changed` semantics.

#### Scenario: Local outputs are redacted
- **WHEN** hook installation or local check diagnostics include token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string text
- **THEN** DataSpec SHALL redact those values from stdout, stderr, and generated template files.
