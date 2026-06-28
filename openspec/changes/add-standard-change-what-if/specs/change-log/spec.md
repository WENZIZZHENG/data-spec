## MODIFIED Requirements

### Requirement: Change Log Query
DataSpec SHALL expose a lightweight backend API to query change logs and support change previews linking users back to relevant log entries after save.

#### Scenario: Query project change logs
- **WHEN** a user queries change logs by project
- **THEN** DataSpec returns a paginated list ordered by `changedAt` descending
- **AND** supports optional `targetType` and `targetId` filters.

#### Scenario: Preview references change log
- **WHEN** a what-if preview has effective changes
- **THEN** the preview includes a rollback hint pointing to the change log entry that will be created after save.
