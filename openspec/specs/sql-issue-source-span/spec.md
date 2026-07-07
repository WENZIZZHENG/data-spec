# sql-issue-source-span Specification

## Purpose
定义 SQL lint issue 的可选源码定位字段、记录持久化和编辑器跳转体验，使可解析问题能定位到行列和 source span。
## Requirements
### Requirement: Lint Issue Location Fields
The system SHALL expose optional source location fields on each SQL lint issue.

#### Scenario: Return line and source span
- **WHEN** a client submits SQL that produces a lint issue with a resolvable table or column reference
- **THEN** the lint issue includes 1-based `line` and `column`
- **AND** the lint issue includes 0-based `sourceStart` and `sourceEnd`

#### Scenario: Keep unresolved location optional
- **WHEN** a lint issue cannot be mapped to a source location
- **THEN** the lint issue remains in the response
- **AND** the location fields are absent or null

### Requirement: Persist Lint Issue Locations
The system SHALL preserve lint issue location fields in SQL check records.

#### Scenario: View record detail with locations
- **WHEN** a client requests SQL check record details
- **THEN** each stored issue includes the same location fields that were returned by the original lint response when available

### Requirement: SQL Lint Page Navigation
The SQL lint page SHALL allow users to navigate from a lint issue to the SQL editor location when a line is available.

#### Scenario: Click issue location
- **WHEN** a lint result issue has `line` and `column`
- **THEN** the page displays a clickable location
- **AND** clicking it moves the SQL editor cursor to that line and column

#### Scenario: Show unresolved location
- **WHEN** a lint result issue has no `line`
- **THEN** the page displays an empty or placeholder location without blocking issue review
