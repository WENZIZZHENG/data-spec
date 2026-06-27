## ADDED Requirements

### Requirement: DDL generation dialect diagnostics
DDL generation SHALL expose the target SQL dialect and compatibility boundary in its result.

#### Scenario: Generated DDL declares PostgreSQL target
- **WHEN** a client generates DDL from a template
- **THEN** the result includes dialect diagnostics identifying `postgresql` as the target dialect
- **AND** the diagnostics state that MySQL execution may require dialect conversion

#### Scenario: DDL lint result preserves diagnostics
- **WHEN** generated DDL is checked by the lint engine
- **THEN** the nested lint result includes dialect diagnostics for the same generated SQL
