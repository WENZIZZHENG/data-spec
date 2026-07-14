## ADDED Requirements

### Requirement: Standard field names coordinate with the active candidate Inbox
DataSpec SHALL serialize standard field creation with active candidate creation for the same project and field name.

#### Scenario: Direct field creation encounters an active candidate
- **WHEN** a caller directly creates a standard field whose project and name are already used by a PENDING or POSTPONED candidate
- **THEN** DataSpec rejects the direct field creation with an actionable Inbox instruction
- **AND** it does not leave a standard field and active same-name candidate concurrently present.

#### Scenario: Candidate acceptance creates the standard field
- **WHEN** a caller accepts a PENDING or POSTPONED candidate
- **THEN** DataSpec may exclude that candidate ID while creating its standard field in the same transaction
- **AND** any other active same-name candidate still blocks the creation.

#### Scenario: Batch creation observes a concurrently committed field
- **WHEN** Starter Kit、复用包、项目恢复或内置标准导入在获取名称锁前读取到字段不存在，但另一个事务先提交了同名字段
- **THEN** DataSpec 在获取整批名称锁后重新查询待创建名称，并复用或跳过已经存在的字段
- **AND** it does not insert a second active field with the same project and name.

#### Scenario: Undo restores a reserved field name
- **WHEN** a field change undo would restore a different name that is reserved by a PENDING or POSTPONED candidate
- **THEN** DataSpec acquires the same project field-name reservation and rejects the undo with an actionable Inbox instruction
- **AND** it leaves the current field name and active candidate unchanged.
