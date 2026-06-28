# ai-decision-replay Specification

## Purpose
TBD - created by archiving change add-ai-decision-replay. Update Purpose after archive.
## Requirements
### Requirement: Persist AI job records
The system SHALL persist lightweight AI job records for DataSpec-generated prompts, SQL lint/fix operations, and DDL preview operations.

#### Scenario: AI job record stores replay metadata
- **WHEN** an AI-related operation is recorded for a project
- **THEN** the record includes projectId, jobType, title, inputSummary, promptVersion, status, inputPayloadJson, outputPayloadJson, and createdAt.
- **AND** promptVersion matches a registered prompt template version for the recorded job type.
- **AND** the record includes standardSnapshotId, standardSnapshotVersion, and standardSnapshotHash when the operation used a versioned standard snapshot.

#### Scenario: Recording rejects invalid payload
- **WHEN** a caller tries to create a job record without projectId or jobType
- **THEN** the system rejects the request with a business validation error.

### Requirement: Integrate replay records with existing AI-facing flows
The system SHALL create replay records from existing DataSpec flows without changing their primary response semantics.

#### Scenario: Create table prompt is recorded
- **WHEN** a user generates a create-table AI prompt
- **THEN** the system records the business description, prompt text, registry promptVersion, and standard snapshot metadata.

#### Scenario: Fix SQL prompt is recorded
- **WHEN** a user generates a SQL fix prompt
- **THEN** the system records the original SQL, lint result summary, prompt text, registry promptVersion, and standard snapshot metadata.

#### Scenario: SQL lint fixedSql is recorded
- **WHEN** a user runs SQL lint for a project and DataSpec produces fixedSql
- **THEN** the system records originalSql, fixedSql, lint issue counts, linked sqlCheckRecordId, registry promptVersion, and standard snapshot metadata.

#### Scenario: DDL preview is recorded
- **WHEN** a user previews DDL from a table template
- **THEN** the system records templateId, tableName, generated DDL, lint result summary, registry promptVersion, and standard snapshot metadata.

#### Scenario: Replay recording failure does not block primary flow
- **WHEN** replay recording fails during prompt generation, lint, or DDL preview
- **THEN** the primary operation still returns its normal result.

### Requirement: Query AI replay records
The system SHALL allow users to query AI replay records for a project and inspect a single replay detail.

#### Scenario: List records by project
- **WHEN** a user requests AI job records for a project
- **THEN** the system returns a paginated list ordered by createdAt descending.
- **AND** each list item includes stable metadata without requiring large input/output payloads.

#### Scenario: View record detail
- **WHEN** a user opens an AI job record detail
- **THEN** the system returns metadata plus parsed inputPayload and outputPayload when the stored JSON is valid.
- **AND** the system returns replayCommand and replayPayload that can be copied.

### Requirement: AI replay frontend flow
The system SHALL provide a frontend flow for reviewing AI job records for the current project.

#### Scenario: View and filter replay records
- **WHEN** a user opens the AI replay page with a selected project
- **THEN** the page loads recent records and lets the user filter by jobType.

#### Scenario: Copy replay context
- **WHEN** a user opens a replay detail
- **THEN** the page displays standard snapshot metadata, input, output, lint summary when present, and provides copy actions for replay payload and replay command.

### Requirement: AI job replay record deduplication
AI job replay records SHALL avoid duplicate rows for the same stable operation input during automatic retries.

#### Scenario: Same AI job fingerprint is recorded twice
- **WHEN** a service attempts to record an AI job with the same project, job type, prompt version, input payload, output payload, snapshot reference, and linked SQL check record
- **THEN** DataSpec reuses the first record instead of inserting a duplicate row.
