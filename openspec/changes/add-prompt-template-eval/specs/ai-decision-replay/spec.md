## MODIFIED Requirements

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
