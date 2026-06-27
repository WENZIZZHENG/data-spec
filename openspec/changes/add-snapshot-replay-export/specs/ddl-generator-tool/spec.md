## ADDED Requirements

### Requirement: DDL replay snapshot metadata
DDL generation SHALL expose enough standard snapshot metadata for future replay.

#### Scenario: DDL result is versioned
- **WHEN** DDL generation returns a result for a project with a standard snapshot
- **THEN** the result includes snapshot ID, version, hash, and source metadata

#### Scenario: CLI prints snapshot metadata
- **WHEN** a caller runs `generate-ddl --format json`
- **THEN** the JSON output preserves the standard snapshot metadata returned by the API
