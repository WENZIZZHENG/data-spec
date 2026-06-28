## MODIFIED Requirements

### Requirement: Latest snapshot metadata
The system SHALL expose latest standard snapshot metadata for a project without requiring callers to parse payload JSON, including for standard change previews.

#### Scenario: Latest snapshot exists
- **WHEN** a caller requests current standard version metadata for a project with snapshots
- **THEN** the system returns snapshot ID, version, name, hash, and created time.

#### Scenario: What-if preview includes snapshot
- **WHEN** a standard change preview is generated for a project with a latest snapshot
- **THEN** the preview includes the latest snapshot metadata and a hint to create a new snapshot after accepting the change.
