## ADDED Requirements

### Requirement: AI profile doctor diagnostics
DataSpec CLI doctor SHALL diagnose repository AI task profile readiness.

#### Scenario: Profile config is valid
- **WHEN** a user runs `doctor --format json` with a valid configured profile
- **THEN** the JSON output includes an `ai-profile` check with status `pass`
- **AND** the check includes the selected profile id, task type, and recommended next command when available.

#### Scenario: Profile config is unknown
- **WHEN** `.dataspec/config.json` references an unknown profile or task type
- **THEN** doctor reports the `ai-profile` check as `fail` or `warn`
- **AND** it suggests supported profile ids or task types.

#### Scenario: Service unavailable
- **WHEN** DataSpec service is unavailable
- **THEN** doctor still reports local profile configuration shape
- **AND** it marks remote profile validation as unavailable rather than throwing an uncaught exception.
