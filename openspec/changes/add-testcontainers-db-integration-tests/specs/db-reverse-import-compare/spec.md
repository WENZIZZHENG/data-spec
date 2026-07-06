## ADDED Requirements

### Requirement: Compare has real database integration coverage
DataSpec SHALL verify database direct compare behavior against real PostgreSQL and MySQL containers through the optional database integration profile.

#### Scenario: Compare classifies matched and unmanaged fields from real metadata
- **WHEN** the integration test compares selected real database tables against a project standard field set
- **THEN** DataSpec SHALL return stable summary counts and table-level diffs for matched, changed, new, missing-comment, and non-standard fields
- **AND** the compare flow SHALL remain read-only against the source database.

#### Scenario: Compare failure identifies dialect context
- **WHEN** a real database compare integration assertion fails
- **THEN** the failing test name or assertion message SHALL identify whether the PostgreSQL or MySQL matrix failed and which metadata field drifted.
