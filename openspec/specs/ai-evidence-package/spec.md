# ai-evidence-package Specification

## Purpose
定义 AI 辅助任务的机器可读证据包，把任务来源、标准快照、输入输出摘要、验证结果、产物和建议命令组织成可交付、可复查的 JSON 契约。
## Requirements
### Requirement: Evidence package model
DataSpec SHALL generate a machine-readable AI evidence package for completed or previewed AI-assisted tasks.

#### Scenario: Generate evidence package JSON
- **WHEN** a client requests an evidence package for a supported source
- **THEN** DataSpec returns `kind`, `schemaVersion`, `packageId`, `projectId`, `generatedAt`, `source`, `standardSnapshot`, `inputsSummary`, `outputsSummary`, `validationSummary`, `artifacts`, `nextActions`, and `suggestedCommands`.
- **AND** the package identifies whether the source is persisted or payload-based.

#### Scenario: Unsupported source type
- **WHEN** a client requests an unsupported evidence source
- **THEN** DataSpec returns a readable validation error that lists supported source types.

### Requirement: Supported evidence sources
DataSpec SHALL support evidence package generation from AI job records, SQL check records, coverage report payloads, and AI batch runs.

#### Scenario: SQL check evidence
- **WHEN** the source is a SQL check record id
- **THEN** the package includes SQL check counts, fixedSql availability, issue summary, replay reference, standard snapshot metadata, and suggested lint/replay commands.

#### Scenario: AI job evidence
- **WHEN** the source is an AI job record id
- **THEN** the package includes job type, status, replay payload summary, standard snapshot metadata, related SQL check id when present, and suggested replay command.

#### Scenario: Coverage report evidence
- **WHEN** the source is a coverage report payload
- **THEN** the package includes coverage summary, table count, unmanaged ranking summary, standard snapshot metadata when provided, and recommended follow-up commands.

#### Scenario: AI batch run evidence
- **WHEN** the source is an AI batch run id
- **THEN** the package includes batch status, item counts, issue summary, fixedSql summary, evidence refs, and suggested retry or export commands.

### Requirement: Evidence zip export
DataSpec SHALL export evidence packages as zip files for handoff.

#### Scenario: Download evidence zip
- **WHEN** a client requests zip output for an evidence package
- **THEN** the response contains `evidence.json`, `summary.md`, and `README.md`.
- **AND** `evidence.json` matches the JSON package structure.

#### Scenario: Summary markdown
- **WHEN** a zip evidence package is generated
- **THEN** `summary.md` describes the source, status, counts, standard version, validation summary, artifacts, and next actions in human-readable Markdown.

### Requirement: Evidence package redaction
Evidence packages MUST NOT expose secrets, full connection strings, or raw business data rows.

#### Scenario: Sensitive content is redacted
- **WHEN** source records, payloads, diagnostics, commands, or errors contain passwords, tokens, Authorization headers, or complete JDBC URLs
- **THEN** the evidence package replaces them with redacted placeholders before returning JSON or zip.

#### Scenario: Business data rows are not included
- **WHEN** the source comes from database metadata, coverage, SQL check, or batch runs
- **THEN** the package includes schema/field/rule/count summaries only
- **AND** it does not include sampled business data rows.

### Requirement: Evidence package can reference task runs
AI evidence packages SHALL be able to include AI task run state for handoff and recovery.

#### Scenario: Evidence from task run
- **WHEN** a caller exports evidence for an AI task run
- **THEN** the package includes task status, failed step, retryable state, resume command, source reference, partial artifacts, validation summary, and next actions.

#### Scenario: Evidence task run redaction
- **WHEN** task run metadata or artifacts are included in an evidence package
- **THEN** the package MUST NOT include token, password, Authorization header, full JDBC URL, or sampled business data rows.

### Requirement: Evidence packages carry post-check summaries
AI evidence packages SHALL be able to include stable reference and post-check summaries without storing raw AI output or secrets.

#### Scenario: Evidence includes validation summary
- **WHEN** an AI task or payload has a post-check result
- **THEN** the evidence package SHALL include status, safeToUse, issue counts, blocking refs, replacement refs, evidence links, and suggested check command
- **AND** existing evidence package fields SHALL remain compatible.

#### Scenario: Evidence excludes unsafe output
- **WHEN** post-check input or issues contain raw AI output, token, password, Authorization, JDBC URL, DSN, or business data rows
- **THEN** the evidence package SHALL store only bounded redacted excerpts and structured reference summaries.

### Requirement: Persisted evidence sources expose resolvable references
AI evidence packages SHALL expose an additive canonical evidence ref for persisted sources and SHALL NOT claim that payload-only sources are independently resolvable.

#### Scenario: Package uses a persisted source
- **WHEN** an evidence package is generated from a SQL check, AI job, AI batch run, or AI task run record
- **THEN** its source includes `evidenceRef` in the format `dataspec://evidence/<source-type>/<source-id>`
- **AND** resolving that ref in the source project verifies the same persisted source.

#### Scenario: Package uses a payload-only source
- **WHEN** an evidence package is generated from a coverage report payload or another non-persisted source
- **THEN** its source `evidenceRef` is empty
- **AND** DataSpec does not fabricate a packageId-based or payload-based verifiable reference.

#### Scenario: Existing evidence clients read the package
- **WHEN** a client that only understands the existing source fields reads a package containing `evidenceRef`
- **THEN** all existing fields and their semantics remain compatible
- **AND** the additive field contains no secret, raw business row, or connection detail.

