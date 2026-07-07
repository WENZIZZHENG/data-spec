# sql-rule-debugger Specification

## Purpose
定义只读 SQL 规则调试端点和前端面板，解释规则启用状态、参数快照、匹配轨迹、修复策略、抑制状态和安全诊断。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint that explains lint rule execution without changing the normal lint result contract.

#### Scenario: Return rule debug traces
- **WHEN** a client posts SQL to `/api/lint/debug`
- **THEN** DataSpec returns a debug result containing the original lint summary and a `rules` array
- **AND** each rule entry includes `ruleCode`, `ruleName`, `severity`, `enabled`, `paramsSnapshot`, `matchTrace`, `sourceRange`, `fixStrategy`, `suppressionStatus`, and `debugNotes`
- **AND** the request does not create or update a SQL check record.

#### Scenario: Explain rules that did not match
- **WHEN** a configured lint rule executes without producing issues
- **THEN** the debug result still includes that enabled rule
- **AND** its `matchTrace` explains that no issue matched under the current SQL and parameter snapshot.

#### Scenario: Explain disabled rules
- **WHEN** project rule configuration disables a lint rule
- **THEN** the debug result includes that rule with `enabled` set to false
- **AND** its `matchTrace` explains that the rule was skipped because project configuration disabled it.

#### Scenario: Keep debug output safe for AI
- **WHEN** the debug result includes rule parameters or notes
- **THEN** DataSpec MUST NOT expose API tokens, passwords, Authorization headers, complete JDBC URLs, or raw source database rows
- **AND** `paramsSnapshot` remains structured so AI agents can reason about rule behavior without parsing prose.

### Requirement: SQL lint page rule debug panel
The SQL lint page SHALL provide a rule debug panel for the current SQL input.

#### Scenario: Show rule execution summary
- **WHEN** a user runs rule debug from the SQL lint page
- **THEN** the page displays each rule's enabled state, severity, match count, suppression state, fix strategy, and source range summary
- **AND** selecting a rule displays its parameter snapshot, match trace, debug notes, and next action guidance.

#### Scenario: Surface debug failures near the panel
- **WHEN** the debug request fails
- **THEN** the page shows a readable error near the debug panel
- **AND** the normal SQL lint result remains visible and unchanged.
