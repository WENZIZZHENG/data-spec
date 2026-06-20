## ADDED Requirements

### Requirement: 数据库直连差异比对
DataSpec SHALL provide a read-only database reverse import compare API that compares selected database tables against the current project's field standards.

#### Scenario: 生成差异摘要
- **WHEN** a user submits database connection information and selected table names to the compare API
- **THEN** the system reads JDBC metadata for those tables
- **AND** it returns table count, column count, matched count, changed count, new count, missing comment count, and non-standard count
- **AND** it does not modify the source database

#### Scenario: 字段命中标准
- **WHEN** a database column matches a DataSpec standard field by name or alias
- **THEN** the compare result marks the field as `MATCHED` when type, nullable, default value, and comment are consistent enough

#### Scenario: 字段属性变化
- **WHEN** a matched database column differs from the standard field in type, nullable, default value, or comment
- **THEN** the compare result marks the field as `CHANGED`
- **AND** it includes changed properties with current database value and standard value

#### Scenario: 新增或非标准字段
- **WHEN** a database column does not match any standard field name or alias
- **THEN** the compare result marks the field as `NEW`
- **AND** it also reports the field as non-standard with a readable reason

### Requirement: 前端差异视图
The reverse import page SHALL allow users to inspect database compare results without leaving the existing direct connection workflow.

#### Scenario: 查看按表分组差异
- **WHEN** a database compare result is returned
- **THEN** the page displays summary metrics and groups field differences by table

#### Scenario: 按状态筛选
- **WHEN** the user chooses a diff status filter
- **THEN** the page shows only field differences matching that status

#### Scenario: 保持确认导入边界
- **WHEN** compare results include new fields
- **THEN** the existing confirmation import flow remains the only path that writes field candidates to DataSpec
- **AND** compare itself remains read-only
