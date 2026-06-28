## MODIFIED Requirements

### Requirement: Standard snapshot creation
The system SHALL allow users to create a project-level standard snapshot for the current fields, enum dictionaries, enum values, and rule configurations.

#### Scenario: Create standard snapshot
- **WHEN** a user creates a snapshot for a project with a version and optional description
- **THEN** the system stores snapshot metadata, deterministic payload JSON, and SHA-256 snapshot hash.
- **AND** the created snapshot is associated with the project.

#### Scenario: Retry snapshot creation with same key
- **WHEN** a caller retries snapshot creation with the same project, version, and idempotency key
- **THEN** DataSpec returns the original snapshot result without creating a duplicate snapshot row.
