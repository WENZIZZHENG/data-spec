# ddl-generator-tool Specification

## Purpose
定义基于 DataSpec 表模板生成 DDL 的工具契约，输出带注释的建表 SQL、lint 自检结果、AI 入口和标准快照元数据。
## Requirements
### Requirement: Template DDL Generation
The system SHALL generate PostgreSQL DDL from a DataSpec table template.

#### Scenario: Generate DDL from template
- **WHEN** a client provides `projectId`, `templateId`, and `tableName`
- **THEN** the system returns PostgreSQL `CREATE TABLE` SQL
- **AND** the SQL includes `COMMENT ON TABLE` and `COMMENT ON COLUMN` statements when comments are available

#### Scenario: AI context exposes format constraints for DDL generation
- **WHEN** an AI or client generates DDL using DataSpec AI Context
- **THEN** the field catalog and database rules SHALL expose standard field format constraints such as units, timezone, precision, regex pattern, valid examples, and invalid examples.
- **AND** the generated DDL workflow can use those constraints without requiring a separate database row scan.

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

### Requirement: DDL preview prompt template version
DDL generation SHALL record its AI replay promptVersion from the prompt template registry.

#### Scenario: DDL replay references registry version
- **WHEN** a user previews DDL from a table template
- **THEN** the AI job record promptVersion matches the DDL preview template registered in the prompt template registry.

### Requirement: DDL and prompt context respects field usage contracts
DDL and AI prompt generation SHALL include field usage contract guidance when standard field context is available.

#### Scenario: Prompt guidance includes usage boundaries
- **WHEN** DDL generation or prompt guidance includes standard field context for fields with usage contracts
- **THEN** the context includes preferred use cases, avoid conditions, join hints, default filters, aggregation hints, replacement guidance, or misuse examples as applicable
- **AND** the guidance tells AI clients not to use fields in avoid conditions without explicit human confirmation

#### Scenario: Usage contract does not mutate generated SQL
- **WHEN** a usage contract is available for a field
- **THEN** DataSpec treats it as read-only guidance for DDL and prompt generation
- **AND** it does not automatically rewrite user SQL, apply database migrations, or modify project fields
