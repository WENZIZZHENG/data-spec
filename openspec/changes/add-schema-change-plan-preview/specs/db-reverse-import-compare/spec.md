## ADDED Requirements

### Requirement: 数据库直连 schema plan 复用比对结果
DataSpec SHALL build database schema change plans from the same schema metadata and standard comparison semantics used by database reverse import compare.

#### Scenario: Plan 与 compare 使用同一表选择
- **WHEN** a caller submits the same database connection and selected table names to compare and schema plan
- **THEN** the schema plan uses the same selected table metadata scope as compare
- **AND** the plan item statuses are derived from matched, changed, missing comment, new, and non-standard compare outcomes.

#### Scenario: Plan 不改变 compare 语义
- **WHEN** schema plan support is added
- **THEN** the existing database compare and dump compare APIs keep their current response shape and read-only behavior.
