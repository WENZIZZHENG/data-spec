# change-log Specification

## Purpose
定义标准字段、枚举和值域、规则配置等项目标准变更的追加式日志和查询能力，为保存后追溯、预览回滚提示和审计提供稳定记录。
## Requirements
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
DataSpec SHALL expose a lightweight backend API to query change logs and support change previews linking users back to relevant log entries after save.

#### Scenario: Query project change logs
- **WHEN** a user queries change logs by project
- **THEN** DataSpec returns a paginated list ordered by `changedAt` descending
- **AND** supports optional `targetType` and `targetId` filters.

#### Scenario: Preview references change log
- **WHEN** a what-if preview has effective changes
- **THEN** the preview includes a rollback hint pointing to the change log entry that will be created after save.
