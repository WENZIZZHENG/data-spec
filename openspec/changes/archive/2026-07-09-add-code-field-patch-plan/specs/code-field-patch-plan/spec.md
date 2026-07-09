## ADDED Requirements

### Requirement: Generate business code patch plan from standard field changes
DataSpec SHALL generate a read-only business code Patch Plan for a requested standard field change inside configured business-repository paths.

#### Scenario: Rename field creates candidate edits
- **WHEN** a user requests a patch plan for renaming field `phone` to `mobile_phone` with explicit scan paths or `.dataspec/config.json` `defaultPaths`
- **THEN** DataSpec returns stable JSON containing `kind`, `schemaVersion`, `change`, `scanSummary`, `candidateEdits[]`, `manualSteps[]`, `riskLevel`, `dryRunResult`, `verificationCommands[]`, `rollbackHint`, `safety`, `diagnostics[]`, and `nextActions[]`.
- **AND** each candidate edit includes `id`, `changeType`, `fileRef`, `reference`, `riskLevel`, `confidence`, `suggestedEdit`, `dryRunDiff`, `requiresHumanReview`, and `reason`.
- **AND** rename candidate edits include the old matched field token and suggested replacement token without writing the target file.

#### Scenario: Type change creates manual review steps
- **WHEN** a user requests a patch plan for changing field `amount` from type `varchar` to `decimal`
- **THEN** DataSpec lists referenced SQL, migration, mapper, model, schema, config, or text files as candidate edits or manual steps.
- **AND** the plan explains that type changes require human review of database migrations, DTO/entity types, serialization, validation, and tests.
- **AND** the plan does not invent an automatic diff when the replacement cannot be determined safely.

#### Scenario: Enum change creates enum review steps
- **WHEN** a user requests a patch plan with enum mappings such as `DRAFT=PENDING`
- **THEN** DataSpec lists candidate files that reference the field and adds manual steps for enum constants, JSON fixtures, validation rules, SQL constraints, and tests.
- **AND** the plan preserves the enum mapping in `change.enumChanges[]` for AI review.

#### Scenario: No references found
- **WHEN** no configured files reference the changed field or its aliases
- **THEN** DataSpec returns `riskLevel=LOW`.
- **AND** `candidateEdits[]` is empty.
- **AND** `nextActions[]` tells the user to confirm scan paths and run project validation before assuming the field is unused.

### Requirement: Classify patch plan risk and verification
DataSpec SHALL classify Patch Plan risk and recommend verification commands without claiming the plan is complete for every framework.

#### Scenario: High risk references
- **WHEN** the field reference index contains high-confidence SQL, DDL, migration, mapper, or model references
- **THEN** the Patch Plan overall `riskLevel` is `HIGH`.
- **AND** the affected candidate edits require human review before any business code change.

#### Scenario: Medium or low risk references
- **WHEN** references are only model, config, or text mentions with medium or low confidence
- **THEN** the Patch Plan sets per-candidate risk from confidence and file kind.
- **AND** the plan explains uncertainty in `reason` or `diagnostics[]`.

#### Scenario: Verification commands are included
- **WHEN** DataSpec generates any Patch Plan
- **THEN** `verificationCommands[]` includes commands or command templates for re-running the plan, re-running `index-refs`, and running the business repository's tests or SQL checks after manual edits.
- **AND** command templates MUST NOT contain token, password, Authorization header, complete JDBC URL, DSN, or connection string values.

### Requirement: Keep patch planning dry-run and non-sensitive
DataSpec SHALL keep field Patch Plan generation read-only and safe to pass to AI.

#### Scenario: Patch planning is read-only
- **WHEN** DataSpec generates a Patch Plan
- **THEN** it reads configured business repository files only.
- **AND** it does not create, modify, delete, stage, commit, push, or apply patches to business repository files.
- **AND** `dryRunResult.willWrite` is `false`.

#### Scenario: Patch planning output is redacted
- **WHEN** a matched line, dry-run diff, diagnostic, verification command, or Markdown output contains token, password, Authorization header, API key, complete JDBC URL, DSN, URL userinfo, or connection-string-like text
- **THEN** DataSpec redacts the sensitive value before writing stdout, stderr, JSON, Markdown, fixture examples, or diagnostics.

#### Scenario: Stop when no scan paths are configured
- **WHEN** a user requests a Patch Plan without explicit paths and no `.dataspec/config.json` `defaultPaths` are configured
- **THEN** DataSpec returns a recoverable diagnostic with code `DATASPEC_DEFAULT_PATHS_MISSING`.
- **AND** DataSpec MUST NOT scan the entire repository.
