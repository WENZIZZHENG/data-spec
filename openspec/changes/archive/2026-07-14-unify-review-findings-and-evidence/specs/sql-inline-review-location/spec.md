## ADDED Requirements

### Requirement: SQL issues map to shared finding locations and waivers
DataSpec SHALL map SQL lint issue location, deterministic fix, and suppression metadata into additive shared findings.

#### Scenario: SQL issue has a source range
- **WHEN** a lint issue has file-relative path and SQL source range metadata
- **THEN** the finding location includes path, line, column, lineEnd, columnEnd, sourceStart, sourceEnd, and locationKind
- **AND** subject preserves project, table, and column semantics.

#### Scenario: SQL issue is suppressed
- **WHEN** a project rule exemption suppresses a lint issue
- **THEN** the finding waiver records waived=true, suppression ID, and bounded reason
- **AND** existing suppressedCount and issue suppression fields remain unchanged.

#### Scenario: SQL fix is deterministic and low risk
- **WHEN** fixedSql applies a LOW-risk deterministic change for an unsuppressed issue
- **THEN** the finding may set autoFixSafe=true
- **AND** planned, skipped, medium/high-risk, or waived fixes do not claim auto-fix safety.
