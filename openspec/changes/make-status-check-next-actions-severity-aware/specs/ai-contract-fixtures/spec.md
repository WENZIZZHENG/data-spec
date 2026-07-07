## ADDED Requirements

### Requirement: Status check next actions reflect current severity
The local status check JSON output SHALL provide next actions that align with the highest severity present in the report so AI agents do not chase nonexistent blocking errors.

#### Scenario: Warning-only report avoids error-first guidance
- **WHEN** the status check emits warning issues and no error issues
- **THEN** the first `nextActions[]` item does not instruct the caller to fix `severity=error`.
- **AND** the first `nextActions[]` item guides the caller to review or resolve warning-level status drift.

#### Scenario: Error report keeps blocking-error guidance
- **WHEN** the status check emits one or more error issues
- **THEN** the first `nextActions[]` item continues to prioritize fixing `severity=error` status drift.

#### Scenario: Clean report keeps no-action guidance
- **WHEN** the status check emits no issues
- **THEN** `nextActions[]` continues to report that no action is needed.
