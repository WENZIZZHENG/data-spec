# dataspec-cli Specification

## Purpose
定义 DataSpec CLI 的核心命令契约，让用户、CI 和 AI Agent 能通过命令行执行 SQL lint、导出 AI Context、生成 prompt 和读取稳定 JSON 输出。
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

### Requirement: CLI evidence export command
The DataSpec CLI SHALL export AI evidence packages for supported sources.

#### Scenario: Export evidence JSON
- **WHEN** a user runs `evidence export --source-type <type> --source-id <id> --format json`
- **THEN** the CLI fetches the evidence package API
- **AND** prints stable JSON with `kind`, `schemaVersion`, `source`, `standardSnapshot`, `validationSummary`, `artifacts`, and `nextActions`.

#### Scenario: Export evidence zip
- **WHEN** a user runs `evidence export --source-type <type> --source-id <id> --format zip --output <path>`
- **THEN** the CLI downloads the zip evidence package to the requested path
- **AND** refuses to write unsafe paths outside the requested workspace target.

#### Scenario: Evidence command failure
- **WHEN** the evidence API returns a validation or not-found error
- **THEN** the CLI exits with code `2`
- **AND** prints a readable error without exposing token, password, Authorization header, or full JDBC URL.

### Requirement: CLI idempotency key forwarding
The DataSpec CLI SHALL let write commands forward an idempotency key to the backend API.

#### Scenario: CLI option sets idempotency key
- **WHEN** a user runs a backend write command with `--idempotency-key`
- **THEN** the CLI sends the value as the `Idempotency-Key` HTTP header.

#### Scenario: CLI environment key fallback
- **WHEN** `DATASPEC_IDEMPOTENCY_KEY` is set and the command does not pass `--idempotency-key`
- **THEN** the CLI sends the environment value as the `Idempotency-Key` HTTP header.

### Requirement: CLI exposes AI capability catalog
The DataSpec CLI SHALL provide commands for AI agents to list, inspect, and validate the server capability catalog.

#### Scenario: List capabilities from CLI
- **WHEN** a user runs `dataspec capability list --format json`
- **THEN** the CLI fetches the server capability catalog and prints stable JSON
- **AND** the output includes capability ids, summaries, surfaces, preflight checks, contracts, examples, and nextActions.

#### Scenario: Show one capability from CLI
- **WHEN** a user runs `dataspec capability show <id> --format json`
- **THEN** the CLI prints the selected capability
- **AND** an unknown id exits with a parameter error and suggests running `capability list`.

#### Scenario: Capability check command
- **WHEN** a user runs `dataspec capability check --format json`
- **THEN** the CLI verifies that required core AI capabilities exist in the catalog
- **AND** reports missing ids or incompatible schemaVersion without executing those capabilities.

#### Scenario: Standard evidence capability fixture
- **WHEN** local CLI capability catalog fixtures are used in tests or AI-readable examples
- **THEN** they include `standard-evidence`
- **AND** `standard-evidence` is read-only, API-only, and lists `GET /api/standard-evidence` without CLI or MCP surfaces.

#### Scenario: Server unavailable
- **WHEN** the CLI cannot reach the DataSpec server while reading capabilities
- **THEN** it returns the existing AI-readable DataSpecError diagnostics
- **AND** suggests running `dataspec doctor --format json`.

### Requirement: Init AGENTS fragment references validation advisor
The DataSpec CLI SHALL include validation advisor guidance in the managed AGENTS fragment generated by `init --with-agents`.

#### Scenario: Write verification guidance into AGENTS fragment
- **WHEN** a user runs `init --with-agents`
- **THEN** the managed AGENTS fragment SHALL mention `node tools/dataspec-verify-advisor.mjs --changed --format json`
- **AND** it SHALL keep existing doctor, lint, and context guidance.

#### Scenario: Refresh verification guidance with force
- **WHEN** a user runs `init --with-agents --force`
- **THEN** the CLI SHALL refresh the managed AGENTS fragment with the current validation advisor guidance.

### Requirement: CLI task run commands
The DataSpec CLI SHALL expose AI task run status for local agents.

#### Scenario: List task runs from CLI
- **WHEN** a user runs `dataspec task list --project <id> --format json`
- **THEN** the CLI fetches project task runs and prints stable JSON with status, taskType, retryable, failedStep, resumeCommand, and source reference.

#### Scenario: List recent failed task runs from CLI
- **WHEN** a user runs `dataspec task failures --project <id> --format json`
- **THEN** the CLI prints recent failed or partially failed task runs with AI-readable next actions.

#### Scenario: Show task run detail from CLI
- **WHEN** a user runs `dataspec task show <id> --project <id> --format json`
- **THEN** the CLI prints parsed step statuses, partial artifacts, metadata, and resume command.

#### Scenario: Task command failure
- **WHEN** the task run API is unavailable or rejects access
- **THEN** the CLI exits with code `2` and prints an existing DataSpecError diagnostic without exposing token, password, Authorization header, or full JDBC URL.

### Requirement: CLI quality gate check
The DataSpec CLI SHALL expose a quality gate check command for CI and AI agents.

#### Scenario: Gate passes from CLI
- **WHEN** a user runs `dataspec quality-gate check --project <id> --format json` and the server returns gate status `PASS`
- **THEN** the CLI prints the gate result JSON and exits with code `0`.

#### Scenario: Gate fails from CLI
- **WHEN** a user runs `dataspec quality-gate check --project <id> --format json` and the server returns gate status `FAIL`
- **THEN** the CLI prints failed checks and next actions as JSON and exits with code `1`.

#### Scenario: Gate command request fails
- **WHEN** the quality gate API is unavailable or rejects access
- **THEN** the CLI exits with code `2` and prints an existing DataSpecError diagnostic without exposing token, password, Authorization header, or full JDBC URL.

### Requirement: CLI session bootstrap command
The DataSpec CLI SHALL expose a `bootstrap` command for AI agents starting a new repository session.

#### Scenario: Bootstrap from server
- **WHEN** a user runs `dataspec bootstrap --project <id> --format json`
- **THEN** the CLI calls `/api/bootstrap/session`
- **AND** it prints the returned bootstrap package as stable JSON without exposing token values.

#### Scenario: Bootstrap server unavailable
- **WHEN** the DataSpec server cannot be reached while running `bootstrap --format json`
- **THEN** the CLI prints a local fallback bootstrap package with `status` set to `BLOCKED`
- **AND** the package contains structured `nextActions` suggesting `dataspec doctor --format json`, service startup, token verification, or project selection as applicable.

#### Scenario: Bootstrap exit code
- **WHEN** the bootstrap status is `READY`
- **THEN** the CLI exits with code `0`
- **AND** when the status is `DEGRADED` or `BLOCKED`, it exits with code `1`.

#### Scenario: Bootstrap text output
- **WHEN** a user runs `dataspec bootstrap --format text`
- **THEN** the CLI prints a concise readable summary of status, project, spec version, recommended commands, risks, and next actions.

### Requirement: CLI Task Card Commands
The DataSpec CLI SHALL expose task-card commands for local AI workflow handoff.

#### Scenario: Create task card
- **WHEN** a user runs `task-card create --workflow <id> --goal <text> --project <id> --format json`
- **THEN** the CLI prints a stable task card JSON object seeded from the workflow recipe.

#### Scenario: Write task card file
- **WHEN** a user passes `--output <path>` to task-card create or update
- **THEN** the CLI writes only the task card JSON or Markdown to that path
- **AND** it refuses to write outside the current working directory unless explicitly supported by existing safe path rules.

#### Scenario: Show task card
- **WHEN** a user runs `task-card show --file <path> --format markdown`
- **THEN** the CLI reads the task card and renders a concise Markdown summary.

#### Scenario: Update task card step
- **WHEN** a user runs `task-card update --file <path> --step <id> --status <status>`
- **THEN** the CLI updates the local task card file without executing workflow steps.

### Requirement: CLI Task Card Exit Codes
The DataSpec CLI SHALL use stable exit codes for task-card commands.

#### Scenario: Task card command succeeds
- **WHEN** a task-card command creates, shows, or updates a valid task card
- **THEN** the CLI exits with code `0`.

#### Scenario: Task card command fails validation
- **WHEN** task-card arguments, file content, workflow id, or step status are invalid
- **THEN** the CLI exits with code `2` and prints a non-sensitive error.

### Requirement: CLI exposes capability safety metadata
The DataSpec CLI SHALL preserve and validate AI write safety metadata in capability commands.

#### Scenario: Capability list preserves safety JSON
- **WHEN** a user runs `dataspec capability list --format json`
- **THEN** the CLI prints the server catalog including each capability `safety` object
- **AND** it does not remove existing `writeRisk`, `preflightChecks`, or `nextActions` fields.

#### Scenario: Capability show text includes safety summary
- **WHEN** a user runs `dataspec capability show <id> --format text`
- **THEN** the CLI prints a concise safety summary including read-only/write status, dry-run requirement, idempotency requirement, sensitive input categories, and next actions.

#### Scenario: Capability check validates safety metadata
- **WHEN** a user runs `dataspec capability check --format json`
- **THEN** the CLI validates that every core capability includes a valid `safety` object with the required fields
- **AND** write-capable capabilities without safety metadata fail the check with a diagnostic instead of being treated as safe.

### Requirement: CLI returns structured safety errors
The DataSpec CLI SHALL surface backend safety diagnostics without exposing secrets.

#### Scenario: CLI receives idempotency safety error
- **WHEN** a CLI write command receives a backend diagnostic for a missing idempotency key
- **THEN** the CLI exits with code `2`
- **AND** stderr includes `DataSpecError` JSON with the backend safety `code`, `category`, `missing`, `safety`, and `nextActions`.

#### Scenario: CLI safety error redaction
- **WHEN** a safety error or argument error contains token, password, Authorization, API key, complete JDBC URL, DSN, or connection string text
- **THEN** the CLI redacts those values from stdout and stderr.

#### Scenario: Existing low-risk commands remain compatible
- **WHEN** an existing read-only or low-risk command does not declare `safety.requiresIdempotencyKey=true`
- **THEN** the CLI does not require a new idempotency key parameter solely because the command has compatible safety metadata.

### Requirement: CLI SQL rule debug command
The DataSpec CLI SHALL expose a `lint-debug` command that returns SQL rule debug output from the server.

#### Scenario: Debug SQL file with JSON output
- **WHEN** a user runs `lint-debug <path> --project <id> --format json`
- **THEN** the CLI reads SQL from the file
- **AND** it calls the configured DataSpec server `/api/lint/debug`
- **AND** it prints the returned debug result as stable JSON.

#### Scenario: Debug SQL from stdin
- **WHEN** a user runs `lint-debug - --project <id> --format json`
- **THEN** the CLI reads SQL from stdin
- **AND** it sends that SQL to `/api/lint/debug`
- **AND** it exits with code `0` when the request succeeds, regardless of lint issue severity.

#### Scenario: Debug request fails
- **WHEN** `lint-debug` arguments are invalid or the server request fails
- **THEN** the CLI exits with code `2`
- **AND** it prints a non-sensitive readable error message to stderr.

### Requirement: CLI version compatibility check
The DataSpec CLI SHALL provide a machine-readable command for checking compatibility with the configured DataSpec server.

#### Scenario: CLI prints compatibility JSON
- **WHEN** a user runs `compat check --format json`
- **THEN** the CLI requests `/api/capabilities/version`
- **AND** it prints the returned compatibility payload as JSON with the local CLI version included.

#### Scenario: CLI reports incompatible server response
- **WHEN** the compatibility payload reports `compatibility.compatible=false`
- **THEN** the CLI exits with code `1`
- **AND** the JSON output includes reasons and next actions for upgrading, downgrading, or stopping.

#### Scenario: CLI compatibility request fails
- **WHEN** the compatibility request cannot reach the DataSpec server or returns an error
- **THEN** the CLI exits with code `2`
- **AND** it prints a readable diagnostic that does not expose tokens, passwords, Authorization headers, JDBC URLs, DSNs, or connection strings.

### Requirement: Doctor includes compatibility summary
The DataSpec CLI doctor command SHALL include version compatibility status in its JSON output.

#### Scenario: Doctor reports compatibility check
- **WHEN** a user runs `doctor --format json`
- **THEN** the result includes a compatibility check with server version, API schema hash, minimum CLI version, status, and next actions when available.

#### Scenario: Doctor remains usable when compatibility check fails
- **WHEN** the compatibility endpoint is unavailable but the server check still returns a reachable response
- **THEN** doctor reports the compatibility check as failed or warning
- **AND** it keeps the existing server, auth, project, default path, and OpenAPI checks in the output.

### Requirement: CLI commands have contract fixture coverage
The DataSpec repository SHALL keep contract fixture entries for high-frequency AI-facing CLI commands.

#### Scenario: Fixture covers core CLI commands
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies fixture entries for high-frequency CLI commands such as `doctor`, `compat check`, `capability list/show/check`, `contract list/show/check`, `lint`, `export-context`, `search-fields`, and `generate-ddl`
- **AND** each entry documents required options, optional options, output shape, exit code semantics, safety metadata, examples, and next actions.

#### Scenario: CLI fixture output shape drifts
- **WHEN** a fixture entry removes a stable output field, exit code, diagnostic shape, or safety note required by the documented CLI contract
- **THEN** the fixture check fails with a readable diagnostic.

### Requirement: CLI contract fixture check is locally runnable
The DataSpec repository SHALL expose a local Node command to validate CLI contract fixtures.

#### Scenario: Run CLI fixture check without service
- **WHEN** a developer runs the CLI/MCP contract fixture check command from the repository root
- **THEN** it validates CLI fixture entries without requiring a running backend, API token, source database, or external network access.

#### Scenario: CLI fixture check joins standard validation
- **WHEN** project Node tests are run through the existing tools test entrypoint
- **THEN** the CLI/MCP contract fixture check is executed or covered by tests so contract fixture drift is caught in normal local validation.

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

### Requirement: CLI Contract Candidate Import Preview
The DataSpec CLI SHALL expose contract candidate import preview as a machine-readable read-only command for AI, CI, and local review workflows.

#### Scenario: Preview contract import as JSON
- **WHEN** a user runs `contract-import preview --project <id> --source-kind <openapi|json-schema|protobuf> --input <path> --format json`
- **THEN** the CLI reads the local contract file
- **AND** calls `/api/contract-import/preview` on the configured DataSpec server
- **AND** prints the returned contract candidate preview package JSON without removing stable fields.

#### Scenario: Preview contract import as text summary
- **WHEN** a user runs `contract-import preview --project <id> --source-kind <sourceKind> --input <path> --format text`
- **THEN** the CLI prints a concise summary containing source kind, contract hash, candidate count, diagnostics, safety summary, and next actions
- **AND** the text output does not become the stable machine-readable contract.

#### Scenario: Contract import command failure
- **WHEN** arguments are invalid, the input file is missing, the source kind is unsupported, or the server request fails
- **THEN** the CLI exits with code `2`
- **AND** it prints a readable diagnostic without exposing token, password, Authorization header, complete JDBC URL, DSN, or connection string values.

### Requirement: CLI contract import fixture coverage
The DataSpec repository SHALL keep contract fixture coverage for the `contract-import preview` command.

#### Scenario: Fixture covers contract import preview command
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies a fixture entry for `contract-import preview`
- **AND** the fixture documents required options, optional options, output shape, exit code semantics, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Fixture rejects unsafe contract import examples
- **WHEN** the contract import fixture includes raw token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** the fixture check fails.

### Requirement: CLI Synthetic Example Generation
The DataSpec CLI SHALL expose synthetic standard example generation as a machine-readable command for AI, CI, and fixture workflows.

#### Scenario: Generate synthetic examples as JSON
- **WHEN** a user runs `synthetic-examples generate --project <id> --scenario <user|order|payment|audit> --format json`
- **THEN** the CLI calls `/api/synthetic-examples/generate` on the configured DataSpec server
- **AND** it prints the returned synthetic example package JSON without removing stable fields.

#### Scenario: Generate synthetic examples as text summary
- **WHEN** a user runs `synthetic-examples generate --project <id> --scenario <scenario> --format text`
- **THEN** the CLI prints a concise summary containing scenario, `specHash`, generated case counts, safety summary, and next actions
- **AND** the text output does not become the stable machine-readable contract.

#### Scenario: Synthetic examples command failure
- **WHEN** arguments are invalid, the scenario is unsupported, or the server request fails
- **THEN** the CLI exits with code `2`
- **AND** it prints a readable diagnostic without exposing token, password, Authorization header, complete JDBC URL, DSN, or connection string values.

### Requirement: CLI synthetic examples fixture coverage
The DataSpec repository SHALL keep contract fixture coverage for the `synthetic-examples generate` command.

#### Scenario: Fixture covers synthetic examples command
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies a fixture entry for `synthetic-examples generate`
- **AND** the fixture documents required options, optional options, output shape, exit code semantics, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Fixture rejects unsafe synthetic examples
- **WHEN** the synthetic examples fixture includes raw token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** the fixture check fails.

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

### Requirement: CLI AI Context budget plan command
The DataSpec CLI SHALL expose a `context-budget plan` command for AI Context budget planning.

#### Scenario: Print budget plan JSON
- **WHEN** a user runs `dataspec context-budget plan --project <id> --token-budget <n> --format json`
- **THEN** the CLI calls the configured DataSpec server budget planner endpoint.
- **AND** it prints the returned budget plan JSON without downloading, caching, or writing AI Context files.
- **AND** it exits with code `0` when the request succeeds.

#### Scenario: Forward scope and task hints
- **WHEN** a user passes `--profile`, `--task-type`, `--scope`, `--query`, `--status`, `--limit`, `--target-table`, or `--target-file`
- **THEN** the CLI forwards those values to the budget planner endpoint.
- **AND** explicit CLI options take precedence over `.dataspec/config.json` profile defaults.

#### Scenario: Budget plan command failure
- **WHEN** arguments are invalid, tokenBudget is missing or non-positive, or the server request fails
- **THEN** the CLI exits with code `2`.
- **AND** stderr contains a non-sensitive diagnostic without exposing token, password, Authorization header, complete JDBC URL, DSN, or connection string.

### Requirement: CLI budget planner fixture coverage
The DataSpec repository SHALL keep contract fixture coverage for the `context-budget plan` command.

#### Scenario: Fixture covers context-budget plan
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies a fixture entry for `context-budget plan`.
- **AND** the fixture documents required options, optional options, output shape, exit code semantics, safety metadata, examples, and recommended next actions.

#### Scenario: Fixture rejects unsafe budget examples
- **WHEN** the budget planner fixture includes raw token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** the fixture check fails.

### Requirement: CLI AI Context 质量检查命令
The DataSpec CLI SHALL expose a local read-only `context-quality check` command that evaluates whether an exported AI Context package or budget plan is sufficient for an AI task.

#### Scenario: Check cached context directory as JSON
- **WHEN** a user runs `dataspec context-quality check --context-dir <dir> --format json`
- **THEN** the CLI reads AI Context files from the directory without calling the DataSpec server.
- **AND** it prints stable JSON containing `kind`, `schemaVersion`, `input`, `contextQualityScore`, `qualityLevel`, `tokenBudgetBreakdown`, `missingCriticalResources`, `truncatedResources`, `coverageByCategory`, `taskFitHints`, and `nextContextActions`.
- **AND** it exits with code `0` when the directory can be evaluated.

#### Scenario: Check context zip as JSON
- **WHEN** a user runs `dataspec context-quality check --context-zip <zip> --format json`
- **THEN** the CLI reads the AI Context zip locally without extracting files outside the workspace.
- **AND** it reports unsafe archive entries as a command failure instead of evaluating them.

#### Scenario: Check budget plan as JSON
- **WHEN** a user runs `dataspec context-quality check --budget-plan <json> --format json`
- **THEN** the CLI reads a `context-budget plan` JSON file locally.
- **AND** it derives score, resource gaps, task fit hints, and next actions from the selected artifacts, dropped artifacts, token estimation, quality risk, fallback steps, and recommended next actions.

#### Scenario: Print human-readable quality summary
- **WHEN** a user runs `dataspec context-quality check` with `--format text`
- **THEN** the CLI prints a concise summary of score, level, missing critical resources, truncated resources, and next actions.
- **AND** the text output is not the stable machine-readable contract.

#### Scenario: Quality command failure
- **WHEN** arguments are invalid, no input source is provided, multiple input sources are provided, an input file is missing, a zip is unsafe, or JSON input cannot be parsed.
- **THEN** the CLI exits with code `2`.
- **AND** stderr contains a non-sensitive diagnostic without exposing token, password, Authorization header, complete JDBC URL, DSN, or connection string values.

### Requirement: CLI AI Context 质量检查 fixture 覆盖
The DataSpec repository SHALL keep contract fixture coverage for the `context-quality check` command.

#### Scenario: Fixture covers context-quality check
- **WHEN** a developer runs the CLI/MCP contract fixture check
- **THEN** it verifies a fixture entry for `context-quality check`.
- **AND** the fixture documents required options, optional options, output shape, exit code semantics, safety metadata, success example, failure example, and recommended next actions.

#### Scenario: Fixture rejects unsafe quality examples
- **WHEN** the context quality fixture includes raw token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values.
- **THEN** the fixture check fails.

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
