## ADDED Requirements

### Requirement: Status check next actions summarize issue codes
The local status check JSON output SHALL include issue code counts and highest severity in the issue-code next action so AI agents can read the immediate recommendation without re-aggregating `issues[]`.

#### Scenario: Warning issue code next action includes count and severity
- **WHEN** the status check emits warning issues with the same code
- **THEN** `nextActions[]` includes the issue code with `count` equal to the repeated issue total.
- **AND** the same next action reports `severity` equal to `warning`.

#### Scenario: Error issue code next action includes count and severity
- **WHEN** the status check emits error issues
- **THEN** `nextActions[]` includes the error code with its count.
- **AND** the same next action reports `severity` equal to `error`.

#### Scenario: Existing action guidance remains stable
- **WHEN** issue code summaries are included in `nextActions[]`
- **THEN** existing `status`, exit code semantics, `summary.issueCodes[]`, `issues[]`, `checks[]`, and the first two next action guidance entries remain unchanged.
