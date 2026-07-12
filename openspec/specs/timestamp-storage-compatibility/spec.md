# timestamp-storage-compatibility Specification

## Purpose
定义 PostgreSQL 时间列与 Java LocalDateTime 模型之间的存储、迁移和历史读取兼容契约。
## Requirements
### Requirement: Local date-time storage compatibility

DataSpec SHALL store application timestamps represented by Java `LocalDateTime` in PostgreSQL columns that do not require an offset-aware Java type.

#### Scenario: Upgrade an existing PostgreSQL database

- **WHEN** Flyway upgrades a database created by any supported earlier migration
- **THEN** application timestamp columns represented by `LocalDateTime` use `timestamp without time zone`
- **AND** existing wall-clock values remain readable through their repositories.

#### Scenario: Create a new PostgreSQL database

- **WHEN** Flyway creates the complete schema in an empty PostgreSQL database
- **THEN** no application-owned `LocalDateTime` column remains defined as `timestamp with time zone`
- **AND** timestamp defaults produce values compatible with `LocalDateTime`.
