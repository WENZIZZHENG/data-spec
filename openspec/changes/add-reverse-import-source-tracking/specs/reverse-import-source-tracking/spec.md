## ADDED Requirements

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
