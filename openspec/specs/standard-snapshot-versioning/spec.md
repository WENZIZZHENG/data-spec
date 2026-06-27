# standard-snapshot-versioning Specification

## Purpose
TBD - created by archiving change add-standard-snapshot-versioning. Update Purpose after archive.
## Requirements
### Requirement: Standard snapshot creation
The system SHALL allow users to create a project-level standard snapshot for the current fields, enum dictionaries, enum values, and rule configurations.

#### Scenario: Create standard snapshot
- **WHEN** a user creates a snapshot for a project with a version and optional description
- **THEN** the system stores snapshot metadata, deterministic payload JSON, and SHA-256 snapshot hash.
- **AND** the created snapshot is associated with the project.

### Requirement: Latest snapshot metadata
The system SHALL expose latest standard snapshot metadata for a project without requiring callers to parse payload JSON.

#### Scenario: Latest snapshot exists
- **WHEN** a caller requests current standard version metadata for a project with snapshots
- **THEN** the system returns snapshot ID, version, name, hash, and created time.

#### Scenario: No snapshot exists
- **WHEN** a caller requests current standard version metadata for a project without snapshots
- **THEN** the system returns an unversioned metadata value and does not block existing workflows.

### Requirement: AI Context version metadata
AI Context exports SHALL include standard snapshot metadata.

#### Scenario: Export AI Context after snapshot
- **WHEN** a project has a latest standard snapshot
- **THEN** AI Context manifest, field catalog, and rules output include the snapshot version and hash.

#### Scenario: Export AI Context without snapshot
- **WHEN** a project has no standard snapshot
- **THEN** AI Context manifest identifies the standard as unversioned.

### Requirement: SQL check record snapshot reference
SQL check records SHALL reference the standard snapshot metadata used during lint.

#### Scenario: Lint with latest snapshot
- **WHEN** SQL lint runs for a project with a latest snapshot
- **THEN** the saved check record contains snapshot ID, version, and hash.

### Requirement: DDL generation snapshot metadata
DDL generation SHALL return the standard snapshot metadata used for lint self-check.

#### Scenario: Generate DDL with latest snapshot
- **WHEN** DDL generation runs for a project with a latest snapshot
- **THEN** the generation response includes snapshot version metadata.
