# ddl-generator-tool Specification

## Purpose
TBD - created by archiving change add-ddl-generator-tool. Update Purpose after archive.
## Requirements
### Requirement: Template DDL Generation
The system SHALL generate PostgreSQL DDL from a DataSpec table template.

#### Scenario: Generate DDL from template
- **WHEN** a client provides `projectId`, `templateId`, and `tableName`
- **THEN** the system returns PostgreSQL `CREATE TABLE` SQL
- **AND** the SQL includes `COMMENT ON TABLE` and `COMMENT ON COLUMN` statements when comments are available

### Requirement: Generated DDL Self Check
Generated DDL SHALL be checked by the existing DataSpec lint engine.

#### Scenario: Return lint result
- **WHEN** DDL is generated
- **THEN** the response includes the generated SQL and the lint result for the same project

### Requirement: AI Tool Entrypoints
DDL generation SHALL be available through API, CLI, and MCP.

#### Scenario: CLI DDL generation
- **WHEN** a user runs `generate-ddl --template <id> --table <name> --project <id> --format json`
- **THEN** the CLI calls the DDL API and prints the JSON result

#### Scenario: MCP DDL generation
- **WHEN** an MCP client calls `generate_table_ddl`
- **THEN** the MCP server calls the DDL API and returns structured JSON content

### Requirement: DDL replay snapshot metadata
DDL generation SHALL expose enough standard snapshot metadata for future replay.

#### Scenario: DDL result is versioned
- **WHEN** DDL generation returns a result for a project with a standard snapshot
- **THEN** the result includes snapshot ID, version, hash, and source metadata

#### Scenario: CLI prints snapshot metadata
- **WHEN** a caller runs `generate-ddl --format json`
- **THEN** the JSON output preserves the standard snapshot metadata returned by the API

### Requirement: DDL generation dialect diagnostics
DDL generation SHALL expose the target SQL dialect and compatibility boundary in its result.

#### Scenario: Generated DDL declares PostgreSQL target
- **WHEN** a client generates DDL from a template
- **THEN** the result includes dialect diagnostics identifying `postgresql` as the target dialect
- **AND** the diagnostics state that MySQL execution may require dialect conversion

#### Scenario: DDL lint result preserves diagnostics
- **WHEN** generated DDL is checked by the lint engine
- **THEN** the nested lint result includes dialect diagnostics for the same generated SQL
