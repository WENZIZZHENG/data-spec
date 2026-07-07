# ai-contract-fixtures Specification

## Purpose
TBD - created by archiving change add-ai-contract-fixtures. Update Purpose after archive.
## Requirements
### Requirement: AI contracts have golden coverage
The system SHALL provide contract fixtures or golden assertions for the primary AI-consumed outputs.

#### Scenario: Backend AI contract output changes
- **WHEN** backend code changes AI Context, SQL lint, fixed SQL, field suggestion, or DDL preview output
- **THEN** existing backend tests detect missing or incompatible stable fields.

#### Scenario: CLI and MCP AI contract output changes
- **WHEN** CLI JSON output or MCP resource/tool output changes
- **THEN** existing Node tests detect missing or incompatible stable fields.

### Requirement: Fixtures cover stable field paths
The contract tests SHALL verify stable field paths, value types, and stable enum values rather than relying only on broad text contains checks.

#### Scenario: Stable field is removed or renamed
- **WHEN** a stable AI-consumed field such as `ruleCode`, `severity`, `fixedSql`, `recommendedName`, `lintResult`, `schemaVersion`, or `recipes` is removed or renamed
- **THEN** contract tests fail with a readable assertion.

#### Scenario: Compatible field is added
- **WHEN** an output adds a new optional field without changing existing stable fields
- **THEN** contract tests continue to pass unless the new field changes documented semantics.

### Requirement: Compatibility policy is documented
The system SHALL document which AI-consumed fields are stable and how compatible additions should be handled.

#### Scenario: AI agent reads compatibility guidance
- **WHEN** a developer or AI agent reads the project documentation
- **THEN** it can identify stable AI contract outputs and understand that additive fields are compatible while removal, rename, or semantic changes require fixture updates.

### Requirement: Contract tests use existing validation entrances
The contract fixtures MUST be executed through the existing project validation commands.

#### Scenario: Developer runs standard validation
- **WHEN** a developer runs `mvn test` or `node --test` for the affected module
- **THEN** the AI contract fixture checks are included without requiring a separate external service.

### Requirement: Prompt template contracts have fixture coverage
The system SHALL verify prompt template registry and prompt output constraints through existing backend validation.

#### Scenario: Prompt template metadata changes
- **WHEN** a prompt template key, version, required section, required phrase, or output format changes
- **THEN** backend tests detect incompatible or incomplete registry metadata.

#### Scenario: Prompt output contract changes
- **WHEN** generated create-table or fix-sql prompt output loses a required section, required phrase, or promptVersion marker
- **THEN** backend tests fail with a readable assertion.

#### Scenario: Golden prompt output changes
- **WHEN** generated prompt text changes from the checked-in golden fixture
- **THEN** backend tests fail and report a readable diff for review.

### Requirement: Contract fixtures cover schema registry
The existing AI contract fixture checks SHALL cover the schema registry and registry consumers.

#### Scenario: Registry contract is incomplete
- **WHEN** a required core contract id, `schemaVersion`, `stableFields`, `jsonSchema`, or compatibility policy is missing
- **THEN** backend contract tests fail with a readable assertion.

#### Scenario: AI Context registry metadata drifts
- **WHEN** AI Context package manifest or `.dataspec/schema-registry.json` loses registry metadata
- **THEN** backend AI Context tests fail.

#### Scenario: CLI and MCP registry output drifts
- **WHEN** CLI contract commands or MCP schema registry resource lose stable registry fields
- **THEN** Node contract tests fail.

### Requirement: Evidence package contract fixtures
The AI contract fixture checks SHALL cover evidence package stable fields and redaction.

#### Scenario: Evidence stable fields drift
- **WHEN** the evidence package loses `kind`, `schemaVersion`, `source`, `standardSnapshot`, `validationSummary`, `artifacts`, `nextActions`, or `suggestedCommands`
- **THEN** backend or Node contract tests fail with a readable assertion.

#### Scenario: Evidence redaction drifts
- **WHEN** an evidence package source contains token, password, Authorization header, or complete JDBC URL values
- **THEN** tests fail if any of those raw sensitive values appear in JSON, Markdown, zip contents, CLI output, or MCP output.

#### Scenario: Evidence zip drifts
- **WHEN** evidence zip generation loses `evidence.json`, `summary.md`, or `README.md`
- **THEN** backend tests fail.

### Requirement: Status check summarizes issue codes
The local status check JSON output SHALL include issue code summaries so AI agents can distinguish repeated instances of the same problem type from distinct problem types.

#### Scenario: Repeated warning code is summarized
- **WHEN** the status check emits multiple warning issues with the same code
- **THEN** `summary.issueCodes[]` contains one item for that code.
- **AND** that item includes the total count and `severity` equal to `warning`.

#### Scenario: Error code is summarized
- **WHEN** the status check emits error issues
- **THEN** `summary.issueCodes[]` contains the error code with its count.
- **AND** that item reports `severity` equal to `error`.

#### Scenario: Existing issue details remain authoritative
- **WHEN** issue code summaries are emitted
- **THEN** existing `issues[]`, `checks[]`, `summary.errors`, `summary.warnings`, `status`, and exit code semantics remain unchanged.

### Requirement: Status check next actions summarize issue codes
The local status check JSON output SHALL include issue code counts and highest severity in the issue-code next action so AI agents can read the immediate recommendation without re-aggregating `issues[]`.

#### Scenario: Warning issue code next action includes count and severity
- **WHEN** the status check emits warning issues with the same code
- **THEN** `nextActions[]` includes the issue code with `count` equal to the repeated issue total.
- **AND** the same next action reports `severity` equal to `warning`.

#### Scenario: Error issue code next action includes count and severity
- **WHEN** the status check emits error issues
- **THEN** `nextActions[]` includes the error code with its count.
- **AND** the same next action reports `severity` equal to `error`.

#### Scenario: Existing action guidance remains stable
- **WHEN** issue code summaries are included in `nextActions[]`
- **THEN** existing `status`, exit code semantics, `summary.issueCodes[]`, `issues[]`, `checks[]`, and the first two next action guidance entries remain unchanged.

### Requirement: Status check reports per-check severity counts
The local status check JSON output SHALL include per-check warning and error counts so AI agents can distinguish warning-only checks from clean checks without re-grouping every issue.

#### Scenario: Warning-only check exposes warning count
- **WHEN** the status check emits a check that has warning issues but no error issues
- **THEN** that check includes `warningCount` greater than zero
- **AND** that check includes `errorCount` equal to zero.

#### Scenario: Error check exposes error count
- **WHEN** the status check emits a check that has error issues
- **THEN** that check includes `errorCount` greater than zero.

#### Scenario: Compatible status semantics
- **WHEN** a warning-only check is emitted
- **THEN** the existing check `status` value remains compatible with the previous no-error semantics.

### Requirement: Status check next actions reflect current severity
The local status check JSON output SHALL provide next actions that align with the highest severity present in the report so AI agents do not chase nonexistent blocking errors.

#### Scenario: Warning-only report avoids error-first guidance
- **WHEN** the status check emits warning issues and no error issues
- **THEN** the first `nextActions[]` item does not instruct the caller to fix `severity=error`.
- **AND** the first `nextActions[]` item guides the caller to review or resolve warning-level status drift.

#### Scenario: Error report keeps blocking-error guidance
- **WHEN** the status check emits one or more error issues
- **THEN** the first `nextActions[]` item continues to prioritize fixing `severity=error` status drift.

#### Scenario: Clean report keeps no-action guidance
- **WHEN** the status check emits no issues
- **THEN** `nextActions[]` continues to report that no action is needed.

### Requirement: Status check text output summarizes check severity counts
The local status check text output SHALL summarize each check's status and severity counts so readers can locate warning-only and error checks without parsing the full issue list.

#### Scenario: Text output includes check summary section
- **WHEN** the status check formats a report as text
- **THEN** the output includes a `检查项:` section before issue details.

#### Scenario: Check summary exposes severity counts
- **WHEN** a check has warning or error issues
- **THEN** its text summary line includes the check id, check status, total issue count, error count, and warning count.

#### Scenario: JSON contract remains unchanged
- **WHEN** status-check output is requested as JSON
- **THEN** the existing JSON fields and status semantics remain unchanged.

### Requirement: Validation advisor prefers single OpenSpec change strict validation
The validation advisor SHALL recommend the narrowest OpenSpec validation command that safely matches the changed paths so AI agents can run fast, relevant checks before broader gates.

#### Scenario: Single active change path
- **WHEN** all OpenSpec input paths belong to the same active change under `openspec/changes/<change-id>/`
- **THEN** the `openspec-validate` recommendation uses `openspec validate <change-id> --strict`.

#### Scenario: Multiple active changes
- **WHEN** OpenSpec input paths include more than one active change id
- **THEN** the `openspec-validate` recommendation uses `openspec validate --all`.

#### Scenario: Main specs or archive paths
- **WHEN** OpenSpec input paths include main specs, archive paths, or paths outside a single active change
- **THEN** the `openspec-validate` recommendation uses `openspec validate --all`.

#### Scenario: Recommendation identity remains stable
- **WHEN** the OpenSpec validation command is narrowed to one change
- **THEN** the recommendation keeps the existing `openspec-validate` command id and category.

### Requirement: Contract import preview contract fixtures
The AI contract fixture checks SHALL cover contract candidate preview package stable fields and redaction.

#### Scenario: Contract import stable fields drift
- **WHEN** backend or CLI output for contract candidate preview loses `kind`, `schemaVersion`, `projectId`, `sourceKind`, `sourcePath`, `contractHash`, `summary`, `candidateFields`, `diagnostics`, `safety`, or `nextActions`
- **THEN** backend or Node contract tests fail with a readable assertion.

#### Scenario: Contract candidate stable fields drift
- **WHEN** a candidate item loses `candidateKey`, `candidateName`, `dataType`, `sourcePath`, `confidence`, `recommendedAction`, `conflictReasons`, or `inboxPayload`
- **THEN** backend or Node contract tests fail with a readable assertion.

#### Scenario: Contract import redaction drifts
- **WHEN** contract import inputs or metadata contain token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** tests fail if any raw sensitive value appears in generated package JSON, CLI output, fixtures, or logs.

### Requirement: Synthetic example contract fixtures
The AI contract fixture checks SHALL cover synthetic standard example package stable fields and redaction.

#### Scenario: Synthetic example stable fields drift
- **WHEN** backend or CLI output for synthetic standard examples loses `kind`, `schemaVersion`, `projectId`, `scenario`, `specHash`, `generationParams`, `goodSql`, `badSql`, `ddlPreviewInputs`, `fieldSuggestionQuestions`, `standardQaCases`, `expectedDiagnostics`, `safety`, or `nextActions`
- **THEN** backend or Node contract tests fail with a readable assertion.

#### Scenario: Synthetic example redaction drifts
- **WHEN** synthetic example inputs or metadata contain token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string values
- **THEN** tests fail if any raw sensitive value appears in generated package JSON, CLI output, fixtures, or logs.
