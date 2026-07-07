# reverse-import-source-tracking Specification

## Purpose
定义数据库直连反向导入的批次和字段来源追踪，让新建标准字段能回溯到来源 schema、表、列、类型、注释和导入统计。
## Requirements
### Requirement: 反向导入批次记录
DataSpec SHALL record a reverse import batch when database direct reverse import candidates are confirmed and at least one new field is created.

#### Scenario: 创建导入批次
- **WHEN** a user confirms database reverse import candidates for a project
- **THEN** the system creates a batch record with project ID, source type, database type, database name, schema name, selected table names, imported count, skipped count, operator, and created time
- **AND** the batch record MUST NOT store the database password or full connection URL

#### Scenario: 只有跳过字段
- **WHEN** all submitted database reverse import candidates already exist as standard fields
- **THEN** the system MAY return skipped results without creating field source records for skipped fields
- **AND** it MUST NOT claim skipped fields were imported by the current batch

### Requirement: 字段来源记录
DataSpec SHALL record source metadata for each standard field created through database direct reverse import.

#### Scenario: 新字段来源
- **WHEN** a reverse import candidate is created as a new standard field
- **THEN** the system records field ID, batch ID, source type, schema name, table name, column name, data type, nullable, default value, comment, and metadata snapshot

#### Scenario: 普通字段不受影响
- **WHEN** a field is created manually or through another path
- **THEN** the field remains valid without any source record

### Requirement: 来源可查询
DataSpec SHALL expose reverse import source information so users can inspect where an imported field came from.

#### Scenario: 查询字段来源
- **WHEN** a user opens or requests source details for a field created by database reverse import
- **THEN** the system returns the source batch summary and field-level source metadata

#### Scenario: 无来源字段
- **WHEN** a user opens or requests source details for a field without source records
- **THEN** the system returns an empty source list or a clear no-source state

### Requirement: 反向导入映射决策记录
DataSpec SHALL persist field-level reverse import mapping decisions for database direct import batches.

#### Scenario: 记录最终映射决策
- **WHEN** a user confirms database reverse import candidates
- **THEN** the system records a decision for each imported, skipped existing, and explicitly ignored candidate
- **AND** each decision stores project ID, batch ID, table name, column name, decision type, matched field identity when available, match reason, confidence, ignore reason, confirm reason, metadata snapshot, and created time.

#### Scenario: 决策记录不保存敏感连接信息
- **WHEN** mapping decisions are persisted
- **THEN** the decision metadata MUST NOT include database password, token, JDBC URL, full connection string, or sampled business data rows.

#### Scenario: 查询映射历史
- **WHEN** a caller queries mapping decisions by project ID or batch ID
- **THEN** the system returns recent decisions ordered by creation time so AI and users can avoid repeating already handled suggestions.
