# snapshot-replay-export Specification

## Purpose
Provide AI-readable historical replay evidence from saved standard snapshots and SQL check records.

## Requirements
### Requirement: Historical snapshot context export
The system SHALL export AI-readable context from a saved standard snapshot without mutating current project standards.

#### Scenario: Export field catalog from snapshot
- **WHEN** a caller requests field catalog export with a valid `projectId` and `snapshotId`
- **THEN** the response uses the saved snapshot payload for fields and enums
- **AND** the response standard metadata identifies the snapshot ID, version, hash, and source as `snapshot`

#### Scenario: Export rules from snapshot
- **WHEN** a caller requests rules export with a valid `projectId` and `snapshotId`
- **THEN** the response uses the saved snapshot payload for rules
- **AND** current project rule edits made after the snapshot do not alter the exported rule list

#### Scenario: Reject cross-project snapshot export
- **WHEN** a caller requests export with a `snapshotId` that belongs to another project
- **THEN** the system rejects the request with a structured validation error

### Requirement: SQL check record replay detail
The system SHALL expose replay metadata for SQL check records that reference a standard snapshot.

#### Scenario: Record has snapshot reference
- **WHEN** a caller requests SQL check record detail for a record with `standardSnapshotId`
- **THEN** the detail includes replay metadata for the recorded snapshot
- **AND** it includes current standard metadata for comparison
- **AND** it includes machine-readable next actions for exporting historical context or rerunning with current standards

#### Scenario: Record has no snapshot reference
- **WHEN** a caller requests SQL check record detail for an unversioned historical record
- **THEN** the detail marks replay status as `unversioned`
- **AND** it keeps the original record and issue detail available

### Requirement: Replay commands
The system SHALL provide copyable commands that help AI agents reproduce historical context.

#### Scenario: Build CLI export command
- **WHEN** replay metadata is returned for a versioned record
- **THEN** it includes a CLI command that exports AI Context for the recorded snapshot
- **AND** the command does not include API tokens, passwords, or full database connection strings
