## ADDED Requirements

### Requirement: 本地文件补丁写入安全
DataSpec SHALL treat AI-assisted local business repository file writes as write-capable operations that require dry-run review and explicit confirmation.

#### Scenario: Local file patch safety metadata
- **WHEN** DataSpec describes the fixedSql file patch operation through CLI fixtures, capability metadata, docs, or task handoff output
- **THEN** the safety metadata marks the operation as not read-only
- **AND** it declares that dry-run review and explicit confirmation are required before writing a local SQL file.

#### Scenario: Local file patch does not expose secrets
- **WHEN** fixedSql patch planning, confirmation, diagnostics, or fixture examples are emitted through CLI, tests, docs, or AI-readable output
- **THEN** the output does not contain raw token, password, Authorization header, API key, complete JDBC URL, DSN, connection string, or source database business rows.

#### Scenario: Local file patch preserves user control
- **WHEN** an AI agent prepares to apply fixedSql to a local file
- **THEN** DataSpec provides a dry-run plan and apply command that require user-visible confirmation
- **AND** DataSpec does not commit, push, or bypass repository review controls.
