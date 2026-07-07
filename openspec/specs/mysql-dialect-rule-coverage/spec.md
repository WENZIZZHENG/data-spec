# mysql-dialect-rule-coverage Specification

## Purpose
定义 MySQL 方言解析和规则兼容覆盖范围，确保常见数值类型、表选项、索引声明和布尔命名规则能被 lint 与预览稳定消费。
## Requirements
### Requirement: MySQL unsigned and decimal parsing
The SQL parser SHALL preserve common MySQL numeric type details for lint and preview consumers.

#### Scenario: Decimal precision and unsigned modifier
- **WHEN** a MySQL `CREATE TABLE` column uses `DECIMAL(p,s) UNSIGNED`
- **THEN** the parsed column data type includes the precision, scale, and unsigned modifier

#### Scenario: Unsigned integer modifier
- **WHEN** a MySQL `CREATE TABLE` column uses an unsigned integer type
- **THEN** the parsed column data type preserves the unsigned modifier

### Requirement: MySQL table options and indexes compatibility
The SQL parser SHALL parse common MySQL table options and index declarations without treating them as fields.

#### Scenario: Engine charset collation and keys
- **WHEN** a MySQL `CREATE TABLE` includes `ENGINE`, `DEFAULT CHARSET`, `COLLATE`, `PRIMARY KEY`, and `KEY`
- **THEN** the parser returns the user-defined columns and table comment without parser errors

### Requirement: MySQL boolean type compatibility for naming rules
The field suffix/prefix type rule SHALL treat MySQL `tinyint(1)` as a boolean-compatible type for boolean naming patterns.

#### Scenario: is prefix with tinyint
- **WHEN** a column named with `is_` uses `tinyint(1)` or `tinyint`
- **THEN** `field_suffix_type` does not report a boolean type mismatch
