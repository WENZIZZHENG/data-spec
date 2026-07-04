## ADDED Requirements

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
