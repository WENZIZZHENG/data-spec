## ADDED Requirements

### Requirement: Capability catalog includes SQL rule debugger
The AI capability catalog SHALL describe the SQL rule debugger as a read-only capability for rule troubleshooting.

#### Scenario: List SQL rule debugger capability
- **WHEN** a caller lists the AI capability catalog
- **THEN** the catalog includes a stable `sql-rule-debugger` capability
- **AND** the capability lists the `/api/lint/debug` API surface, CLI `lint-debug` surface, output contract, preflight checks, and next actions.

#### Scenario: SQL rule debugger safety
- **WHEN** the catalog describes `sql-rule-debugger`
- **THEN** it marks the capability as read-only
- **AND** it explains that the capability does not save SQL check records, change rules, create suppressions, or mutate project state.
