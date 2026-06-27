## ADDED Requirements

### Requirement: SQL check replay metadata
SQL check record detail SHALL include standard snapshot replay metadata when available.

#### Scenario: Detail includes replay metadata
- **WHEN** a client requests a SQL check record detail
- **THEN** the response includes `replay.recordedStandard`, `replay.currentStandard`, `replay.status`, and `replay.nextActions`
- **AND** existing `record` and `issues` fields remain compatible

#### Scenario: Current standard differs
- **WHEN** the record snapshot hash differs from the current standard hash
- **THEN** replay status identifies that the record used a historical standard
- **AND** next actions include exporting historical context before applying fixes
