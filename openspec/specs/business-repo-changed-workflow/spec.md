# business-repo-changed-workflow Specification

## Purpose
定义 DataSpec CLI 在业务仓库中面向 AI 的变更感知入口：基于 git 变更和 `.dataspec/config.json` 的 `defaultPaths` 发现相关文件，只对本次 SQL 变更运行 lint，并输出最小 AI Context 建议。

## Requirements
### Requirement: Changed File Discovery
DataSpec CLI SHALL provide a `changed` command that discovers current business-repo changes inside configured DataSpec paths.

#### Scenario: Discover changed SQL within default paths
- **GIVEN** `.dataspec/config.json` contains `defaultPaths`
- **AND** the current git worktree has changed or untracked `.sql` files under those paths
- **WHEN** an AI runs `dataspec changed --format json`
- **THEN** the output SHALL include the matching changed files
- **AND** it SHALL classify `.sql` files separately from other changed files
- **AND** it SHALL include summary counts and next commands for linting and minimal context export.

#### Scenario: Do not scan outside configured paths
- **GIVEN** `.dataspec/config.json` contains `defaultPaths`
- **AND** the git worktree has changed files outside those paths
- **WHEN** an AI runs `dataspec changed --format json`
- **THEN** those outside files SHALL NOT be included in the lint candidate list
- **AND** the output SHALL include them only as ignored counts or diagnostics, not as files to scan.

#### Scenario: Recoverable no git repository
- **GIVEN** the command runs outside a git repository
- **WHEN** an AI runs `dataspec changed --format json`
- **THEN** the command SHALL return a machine-readable diagnostic with code `NO_GIT_REPOSITORY`
- **AND** it SHALL include a recovery suggestion
- **AND** it SHALL NOT call the DataSpec server.

#### Scenario: Recoverable no configured paths
- **GIVEN** no `.dataspec/config.json` exists or `defaultPaths` is empty
- **WHEN** an AI runs `dataspec changed --format json`
- **THEN** the command SHALL return a machine-readable diagnostic with code `DATASPEC_DEFAULT_PATHS_MISSING`
- **AND** it SHALL include a recovery suggestion
- **AND** it SHALL NOT scan the full repository.

### Requirement: Changed SQL Lint Workflow
DataSpec CLI SHALL provide a `lint-changed` command that lints only SQL files discovered by the changed-file workflow.

#### Scenario: Lint only changed SQL files
- **GIVEN** changed-file discovery finds SQL and non-SQL files
- **WHEN** an AI runs `dataspec lint-changed --format json`
- **THEN** DataSpec SHALL call the existing lint API only for changed SQL files
- **AND** the output SHALL include the changed-file summary, lint summary, per-file lint results, and context recommendation
- **AND** the command exit code SHALL match existing `lint-files` semantics for lint errors.

#### Scenario: No changed SQL files
- **GIVEN** changed-file discovery finds no SQL files
- **WHEN** an AI runs `dataspec lint-changed --format json`
- **THEN** the command SHALL not call the DataSpec server
- **AND** it SHALL return a recoverable diagnostic with code `NO_CHANGED_SQL_FILES`
- **AND** it SHALL include next actions for `dataspec changed` and minimal context export.

### Requirement: Minimal Context Recommendation
DataSpec CLI SHALL recommend a minimal AI Context command derived from changed files.

#### Scenario: Recommend changed scope context
- **GIVEN** changed-file discovery finds at least one relevant file
- **WHEN** an AI runs `dataspec changed --format json`
- **THEN** the output SHALL include `contextRecommendation.scope` as `changed`
- **AND** it SHALL include a deterministic query derived from changed file names or directories
- **AND** it SHALL include an `export-context` command using `--scope changed --query <query>`.
