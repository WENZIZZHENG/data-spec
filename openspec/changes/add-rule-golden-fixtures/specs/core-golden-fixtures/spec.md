## ADDED Requirements

### Requirement: Core fixture test resources
DataSpec SHALL keep reusable core SQL and reverse-import examples as test resources that are executed by the backend test suite.

#### Scenario: SQL fixtures are parsed and linted
- **WHEN** `mvn test` runs backend tests
- **THEN** PostgreSQL and MySQL SQL fixture files are parsed through the existing SQL parser
- **AND** lint assertions verify key rule outcomes for good and bad examples

#### Scenario: fixedSql golden output is compared
- **WHEN** a SQL fixture has deterministic lint fixes
- **THEN** the generated `fixedSql` is compared against a golden SQL file
- **AND** changing the generated SQL requires changing the golden file in review

#### Scenario: reverse import metadata fixture is compared
- **WHEN** reverse import fixture metadata is loaded
- **THEN** the reverse import preview reports stable candidate, missing-comment, and non-standard-field summaries
- **AND** the fixture is parsed as structured JSON instead of ad hoc string checks
