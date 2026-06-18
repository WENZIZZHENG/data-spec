## ADDED Requirements

### Requirement: Standard Change Logging

DataSpec SHALL record standard data changes in an append-only change log.

#### Scenario: Record field update

- **WHEN** a standard field is updated
- **THEN** DataSpec records `targetType = field`, `targetId`, `action = update`
- **AND** records both `beforeJson` and `afterJson`
- **AND** records `changedAt`.

#### Scenario: Record code set and rule changes

- **WHEN** an enum dictionary, enum value, or rule config is created, updated, deleted, or toggled
- **THEN** DataSpec records the corresponding target type, target ID, action, and before/after snapshots.

### Requirement: Change Log Query

DataSpec SHALL expose a lightweight backend API to query change logs.

#### Scenario: Query project change logs

- **WHEN** a user queries change logs by project
- **THEN** DataSpec returns a paginated list ordered by `changedAt` descending
- **AND** supports optional `targetType` and `targetId` filters.
