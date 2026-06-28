## ADDED Requirements

### Requirement: Reverse import preview from schema dump
DataSpec SHALL support reverse import preview from a database schema dump without reconnecting to the source database.

#### Scenario: Preview dump tables
- **WHEN** a caller submits projectId and a valid schema dump to the dump preview API
- **THEN** DataSpec converts the dump to table definitions
- **AND** returns the same reverse import preview structure used by SQL and database direct preview.

#### Scenario: Dump preview remains read-only
- **WHEN** a caller previews a schema dump
- **THEN** DataSpec does not write to the source database or standard field library
- **AND** candidate import still requires the existing explicit confirmation flow.
