## ADDED Requirements

### Requirement: MySQL Create Table Parsing

DataSpec SHALL parse common MySQL `CREATE TABLE` DDL.

#### Scenario: Parse MySQL table and column comments

- **WHEN** SQL contains backtick identifiers, `AUTO_INCREMENT`, inline column `COMMENT`, and table `COMMENT='...'`
- **THEN** DataSpec returns the table name, table comment, column names, data types, nullability, default values, and column comments.
