# db-reverse-import-compare Specification

## Purpose
定义数据库直连反向导入的只读差异比对能力，帮助用户按表查看数据库字段与当前项目标准之间的匹配、变化、新增和非标准状态。
## Requirements
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

### Requirement: Database compare from schema dump
DataSpec SHALL compare a database schema dump against current project field standards without reconnecting to the source database.

#### Scenario: Compare dump metadata
- **WHEN** a caller submits projectId and a valid schema dump to the dump compare API
- **THEN** DataSpec converts the dump to table definitions
- **AND** returns the same compare summary and table-level field diffs used by database direct compare.

#### Scenario: Dump compare is reproducible
- **WHEN** a dump was generated from selected live database tables
- **THEN** comparing that dump produces the same matched, changed, new, missing comment, and non-standard counts as comparing the same selected tables through direct connection, assuming project standards did not change.

### Requirement: 数据库直连 schema plan 复用比对结果
DataSpec SHALL build database schema change plans from the same schema metadata and standard comparison semantics used by database reverse import compare.

#### Scenario: Plan 与 compare 使用同一表选择
- **WHEN** a caller submits the same database connection and selected table names to compare and schema plan
- **THEN** the schema plan uses the same selected table metadata scope as compare
- **AND** the plan item statuses are derived from matched, changed, missing comment, new, and non-standard compare outcomes.

#### Scenario: Plan 不改变 compare 语义
- **WHEN** schema plan support is added
- **THEN** the existing database compare and dump compare APIs keep their current response shape and read-only behavior.

### Requirement: Compare has real database integration coverage
DataSpec SHALL verify database direct compare behavior against real PostgreSQL and MySQL containers through the optional database integration profile.

#### Scenario: Compare classifies matched and unmanaged fields from real metadata
- **WHEN** the integration test compares selected real database tables against a project standard field set
- **THEN** DataSpec SHALL return stable summary counts and table-level diffs for matched, changed, new, missing-comment, and non-standard fields
- **AND** the compare flow SHALL remain read-only against the source database.

#### Scenario: Compare failure identifies dialect context
- **WHEN** a real database compare integration assertion fails
- **THEN** the failing test name or assertion message SHALL identify whether the PostgreSQL or MySQL matrix failed and which metadata field drifted.
