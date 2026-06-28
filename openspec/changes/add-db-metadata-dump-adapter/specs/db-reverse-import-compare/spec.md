## ADDED Requirements

### Requirement: Database compare from schema dump
DataSpec SHALL compare a database schema dump against current project field standards without reconnecting to the source database.

#### Scenario: Compare dump metadata
- **WHEN** a caller submits projectId and a valid schema dump to the dump compare API
- **THEN** DataSpec converts the dump to table definitions
- **AND** returns the same compare summary and table-level field diffs used by database direct compare.

#### Scenario: Dump compare is reproducible
- **WHEN** a dump was generated from selected live database tables
- **THEN** comparing that dump produces the same matched, changed, new, missing comment, and non-standard counts as comparing the same selected tables through direct connection, assuming project standards did not change.
