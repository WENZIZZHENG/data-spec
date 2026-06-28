## ADDED Requirements

### Requirement: Project Activity Timeline API

DataSpec SHALL provide a read-only project activity timeline API that aggregates existing project records without creating an enterprise audit workflow.

#### Scenario: List recent project activities

- **WHEN** a user queries recent activities for a project
- **THEN** DataSpec returns activities sorted by occurredAt descending
- **AND** each activity includes id, actionType, title, description, occurredAt, actor, source, severity, detailRoute, and safe metadata.

#### Scenario: Filter activities by action type

- **WHEN** a user queries recent activities with an actionType filter
- **THEN** DataSpec returns only activities whose actionType matches the requested type.

#### Scenario: Limit result size

- **WHEN** a user queries activities with a large limit
- **THEN** DataSpec caps the result size to the configured service maximum.

### Requirement: Activity Sources

DataSpec SHALL include available activity entries from existing field change logs, standard snapshots, reverse import batches, SQL check records, AI job records, and API token usage summaries.

#### Scenario: Aggregate existing sources

- **WHEN** a project has records in multiple supported sources
- **THEN** the activity API returns a single normalized timeline containing entries from those sources.

#### Scenario: Exclude unavailable sources

- **WHEN** an activity source has no persisted record for the project
- **THEN** DataSpec omits that source without failing the timeline request.

### Requirement: Safe Activity Metadata

Project activity metadata SHALL contain only safe summary fields and MUST NOT expose SQL text, token values, token hashes, database passwords, or connection strings.

#### Scenario: Return token usage activity for all-project caller

- **WHEN** a caller with all-project access queries activity timeline and a token usage summary appears
- **THEN** the activity metadata contains only safe identifiers and scope summary
- **AND** it does not contain token secret material.

#### Scenario: Hide token usage activity for project-scoped caller

- **WHEN** a project-scoped caller queries activity timeline
- **THEN** DataSpec does not expose API token usage activities.

#### Scenario: Return SQL check activity

- **WHEN** a SQL check record appears in the activity timeline
- **THEN** the activity metadata contains issue counts and record identifiers
- **AND** it does not contain raw SQL content.
