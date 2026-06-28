## ADDED Requirements

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
