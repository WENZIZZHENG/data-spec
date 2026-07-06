## ADDED Requirements

### Requirement: Security diagnostics have real database integration coverage
DataSpec SHALL verify database connection security and health diagnostics against real PostgreSQL and MySQL containers through the optional database integration profile.

#### Scenario: Read-only users are diagnosed as safe
- **WHEN** the PostgreSQL or MySQL integration fixture connects using a least-privilege read-only user
- **THEN** DataSpec SHALL return success with health connection status, dialect capability, current user, readable scope counts, readOnly/writeRisk indicators, and safe or non-dangerous risk guidance.

#### Scenario: Diagnostics remain secret-safe
- **WHEN** a real database connection diagnostic is produced by the integration tests
- **THEN** test assertions and generated evidence MUST NOT expose raw passwords, bearer tokens, full JDBC URLs, DSNs, or source database row values.
