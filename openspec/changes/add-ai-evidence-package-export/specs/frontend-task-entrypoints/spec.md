## ADDED Requirements

### Requirement: Evidence package front-end actions
DataSpec Web SHALL expose lightweight evidence package actions on high-frequency result views.

#### Scenario: SQL check evidence action
- **WHEN** a user views a SQL check result or record detail
- **THEN** the UI offers a way to copy evidence JSON or download an evidence zip for that SQL check.

#### Scenario: Coverage report evidence action
- **WHEN** a user views a field coverage report
- **THEN** the UI offers a way to copy evidence JSON or download an evidence zip for the current coverage report summary.

#### Scenario: AI batch evidence action
- **WHEN** a user views an AI batch run result or detail
- **THEN** the UI offers a way to copy evidence JSON or download an evidence zip for that batch run.

#### Scenario: Evidence action state
- **WHEN** no current project or source result is available
- **THEN** the evidence action is disabled or hidden with a clear non-sensitive state.
