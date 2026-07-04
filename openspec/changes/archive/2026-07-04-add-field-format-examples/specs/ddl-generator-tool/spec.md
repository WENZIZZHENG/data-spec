## MODIFIED Requirements

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
