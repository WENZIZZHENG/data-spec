# openspec-readiness-check Specification

## Purpose
定义 DataSpec 本地 OpenSpec change 准备度检查能力，帮助开发者和 AI agent 在实施前确认 proposal、design、spec 和 tasks 是否具备足够事实、边界、验证计划和评审提示。

## Requirements
### Requirement: OpenSpec readiness check command
DataSpec SHALL provide a local read-only OpenSpec readiness checker for repo-local active changes.

#### Scenario: Report ready change as JSON
- **WHEN** a developer runs `node tools/dataspec-openspec-readiness.mjs --change <change-id> --format json`
- **THEN** the tool reads the change artifacts from `openspec/changes/<change-id>` without modifying files.
- **AND** it prints stable JSON containing `kind`, `schemaVersion`, `change`, `readinessScore`, `readinessLevel`, `missingFacts`, `affectedSpecs`, `validationPlan`, `reviewBoundary`, `riskFlags`, `humanQuestions`, `checks`, and `nextActions`.
- **AND** it exits with code `0` when a report can be generated.

#### Scenario: Report incomplete change gaps
- **WHEN** proposal, design, specs, or tasks are missing required implementation facts such as goals, non-goals, impact, acceptance criteria, affected specs, validation commands, review boundary, or open questions.
- **THEN** the report includes deterministic `missingFacts` entries with code, severity, file, line when available, message, and suggested fix.
- **AND** the report lowers `readinessScore` and sets `readinessLevel` to `NEEDS_REVIEW` or `BLOCKED` according to gap severity.
- **AND** a spec delta only counts as affected when it includes an `ADDED`, `MODIFIED`, or `REMOVED Requirements` section with at least one `Requirement`.
- **AND** empty `Impact` sections and line-level placeholder text such as bare `TODO` or `TBD` are reported as gaps while product phrases such as `TODO 到 OpenSpec` are not treated as placeholders.

#### Scenario: Redact validation plan commands
- **WHEN** artifacts contain validation commands with credentials, URL userinfo, bearer tokens, JDBC URLs, DSNs, or password-like options.
- **THEN** the readiness report includes only redacted commands in `validationPlan`.
- **AND** JSON and text output do not reveal the raw secret-like values.

#### Scenario: Preserve advisory boundary
- **WHEN** a change has a low readiness score.
- **THEN** the tool still exits with code `0` after printing the report.
- **AND** it does not run implementation, tests, `openspec validate`, commit, archive, or write generated artifacts.

#### Scenario: Readiness command failure
- **WHEN** arguments are invalid, the requested change id is unsafe, the change directory does not exist, or the change is archived.
- **THEN** the tool exits with code `2`.
- **AND** stderr contains a non-sensitive diagnostic.
- **AND** archive detection only treats an exact archived change id, with or without the date prefix, as archived.

### Requirement: OpenSpec readiness text output
DataSpec SHALL provide a concise human-readable readiness report.

#### Scenario: Print text summary
- **WHEN** a developer runs `node tools/dataspec-openspec-readiness.mjs --change <change-id> --format text`
- **THEN** the tool prints the score, level, affected specs, missing facts, validation plan, human questions, and next actions.
- **AND** the text output is not the stable machine-readable contract.

### Requirement: OpenSpec readiness test coverage
The DataSpec repository SHALL keep local Node test coverage for the readiness checker.

#### Scenario: Tests cover ready and incomplete changes
- **WHEN** `node --test tools/dataspec-openspec-readiness.test.mjs` runs.
- **THEN** it covers a ready change, an incomplete change with missing acceptance or validation commands, unsafe or missing change ids, advisory low-score exit behavior, placeholder detection, effective spec delta detection, validation command redaction, empty Impact detection, and exact archive matching.
