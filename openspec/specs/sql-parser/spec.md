# sql-parser Specification

## Purpose
TBD - created by archiving change support-mysql-create-table. Update Purpose after archive.
## Requirements
### Requirement: MySQL Create Table Parsing

DataSpec SHALL parse common MySQL `CREATE TABLE` DDL.

#### Scenario: Parse MySQL table and column comments

- **WHEN** SQL contains backtick identifiers, `AUTO_INCREMENT`, inline column `COMMENT`, and table `COMMENT='...'`
- **THEN** DataSpec returns the table name, table comment, column names, data types, nullability, default values, and column comments.

### Requirement: Parser dialect diagnostics
SQL parsing SHALL expose dialect diagnostics for PostgreSQL/MySQL compatibility boundaries.

#### Scenario: Parsed SQL includes MySQL-only features
- **WHEN** parsing SQL containing MySQL table options, backtick identifiers, `AUTO_INCREMENT`, or inline `COMMENT`
- **THEN** DataSpec returns a MySQL dialect diagnostic
- **AND** any capability that is parsed through a compatibility path is marked as partial or informational

#### Scenario: Parser cannot verify vendor-specific syntax
- **WHEN** SQL contains vendor-specific syntax that DataSpec does not verify
- **THEN** diagnostics include a stable code and next action instead of silently marking the syntax supported
