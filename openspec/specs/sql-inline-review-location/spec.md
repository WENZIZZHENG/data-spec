# sql-inline-review-location Specification

## Purpose
定义用于文件级 review 的稳定 SQL issue source range，在常见 PostgreSQL/MySQL DDL 中把表级和字段级问题定位到准确标识符范围。
## Requirements
### Requirement: 稳定 SQL issue source range
DataSpec SHALL provide stable source range metadata for SQL lint issues when the related table or column can be located in the submitted SQL text.

#### Scenario: 表级问题定位到表名
- **WHEN** a SQL lint rule reports an issue with `tableName` and no `columnName`
- **THEN** the issue includes `line`, `column`, `lineEnd`, `columnEnd`, `sourceStart`, `sourceEnd`, and `locationKind`
- **AND** the range points to the table identifier in the matching `CREATE TABLE` statement

#### Scenario: 字段级问题定位到字段名
- **WHEN** a SQL lint rule reports an issue with both `tableName` and `columnName`
- **THEN** the issue source range points to the column identifier inside the matching table definition
- **AND** the resolver MUST prefer the matching table definition over the same column name in another table

#### Scenario: 无法定位的问题
- **WHEN** an issue has no locatable table or column reference
- **THEN** DataSpec keeps the location fields empty instead of returning an arbitrary fallback range

### Requirement: 常见 DDL 写法定位兼容
DataSpec SHALL resolve source ranges for common PostgreSQL and MySQL DDL forms used by this project.

#### Scenario: schema 前缀与引号标识符
- **WHEN** SQL contains `CREATE TABLE schema.table`, quoted identifiers, MySQL backtick identifiers, or square-bracket identifiers
- **THEN** table and column issues are located on the matching identifier text

#### Scenario: 多语句与多表 SQL
- **WHEN** SQL contains multiple `CREATE TABLE` statements with overlapping column names
- **THEN** a column issue is located in the table named by the issue, not the first matching column in the whole file

#### Scenario: COMMENT ON 语句
- **WHEN** SQL contains separate `COMMENT ON TABLE` or `COMMENT ON COLUMN` statements
- **THEN** DataSpec can use those statements as location candidates for comment-related issues when they better represent the offending source

### Requirement: CLI review 输出文件级定位
DataSpec CLI SHALL expose issue file locations in machine-readable and human-readable review outputs.

#### Scenario: lint-files JSON 输出
- **WHEN** `dataspec lint-files --format json` processes SQL files with locatable issues
- **THEN** each file result includes issues with file path plus the issue line/range fields returned by the server

#### Scenario: PR review Markdown 输出
- **WHEN** `dataspec review-pr` builds a PR review comment from locatable issues
- **THEN** each issue line includes a readable file-relative location such as `行 2:5-2:11`
- **AND** the command creates or updates the single DataSpec summary comment on every run
- **AND** issues mapped to changed PR lines are eligible for deduplicated inline comments while unmapped or duplicate issues remain in the summary with fallback reasons.
