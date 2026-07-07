## ADDED Requirements

### Requirement: Status check reports per-check severity counts
The local status check JSON output SHALL include per-check warning and error counts so AI agents can distinguish warning-only checks from clean checks without re-grouping every issue.

#### Scenario: Warning-only check exposes warning count
- **WHEN** the status check emits a check that has warning issues but no error issues
- **THEN** that check includes `warningCount` greater than zero
- **AND** that check includes `errorCount` equal to zero.

#### Scenario: Error check exposes error count
- **WHEN** the status check emits a check that has error issues
- **THEN** that check includes `errorCount` greater than zero.

#### Scenario: Compatible status semantics
- **WHEN** a warning-only check is emitted
- **THEN** the existing check `status` value remains compatible with the previous no-error semantics.
