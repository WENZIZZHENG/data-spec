## ADDED Requirements

### Requirement: Snapshot payload lookup
The system SHALL allow services to load a saved standard snapshot payload by ID or version within the owning project.

#### Scenario: Load snapshot by id
- **WHEN** a service requests a snapshot by `projectId` and `snapshotId`
- **THEN** the system returns snapshot metadata and parsed payload JSON
- **AND** the payload hash matches the stored snapshot hash

#### Scenario: Load snapshot by version
- **WHEN** a service requests a snapshot by `projectId` and version
- **THEN** the system returns the matching snapshot metadata and parsed payload JSON

#### Scenario: Snapshot does not belong to project
- **WHEN** a service requests a snapshot ID outside the current project
- **THEN** the system rejects the request and does not return payload content
