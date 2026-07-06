## ADDED Requirements

### Requirement: Coverage report has real database integration coverage
DataSpec SHALL verify database direct coverage behavior against real PostgreSQL and MySQL containers through the optional database integration profile.

#### Scenario: Coverage uses real metadata without source rows
- **WHEN** the integration test generates a coverage report from selected real database tables
- **THEN** DataSpec SHALL classify standard matches, alias matches, missing comments, possible duplicates, and unmanaged fields from schema metadata
- **AND** it MUST NOT read or require source database business rows.

#### Scenario: Coverage summary remains stable across dialects
- **WHEN** PostgreSQL and MySQL fixtures contain equivalent standard and unmanaged columns
- **THEN** DataSpec SHALL return deterministic coverage summary fields such as tableCount, columnCount, coveredCount, unmanagedCount, missingCommentCount, and coverageRate for each dialect.
