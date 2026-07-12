## ADDED Requirements

### Requirement: DDL generation consumes table structure standards
DDL generation SHALL consume additive table structure standards when generating preview SQL from a template.

#### Scenario: Generate DDL with primary and unique constraints
- **WHEN** a client previews DDL for a template with a `primaryKey` and `uniqueKeys`
- **THEN** the generated PostgreSQL DDL includes safe structured `PRIMARY KEY` and `UNIQUE` constraints for the referenced template columns
- **AND** the response includes a structure standard summary naming which constraints were applied.

#### Scenario: Generate DDL with indexes and foreign key hints
- **WHEN** a client previews DDL for a template with structured `indexes` or `foreignKeys`
- **THEN** the generated PostgreSQL DDL includes safe `CREATE INDEX` statements and foreign key constraints when all referenced columns and target references are valid
- **AND** invalid or advisory-only hints are returned as diagnostics rather than raw SQL.

#### Scenario: Policy guidance remains read-only
- **WHEN** a template has `checkHints`, `auditPolicy`, `softDeletePolicy`, `dialectNotes`, or `aiUsageNotes`
- **THEN** DDL generation returns those policies in the structure standard summary
- **AND** it does not execute database migrations, connect to a source database, or apply changes.

### Requirement: DDL structure evidence
DDL preview SHALL return explainable evidence for table structure standards used during generation.

#### Scenario: Return evidence with lint result
- **WHEN** DDL preview consumes a business object standard or table structure standard
- **THEN** the result includes `structureSummary` with applied constraints, generated indexes, skipped hints, policy notes, and evidence references
- **AND** relation hints remain available through the business object relation summary or table standards context
- **AND** existing `ddl`, `lintResult`, `standardSnapshot`, and `dialectDiagnostics` fields remain present.

#### Scenario: AI replay records structure context
- **WHEN** a DDL preview is recorded as an AI job
- **THEN** the replay output payload includes a non-sensitive structure summary
- **AND** it excludes raw secrets, JDBC URLs, DSNs, Authorization headers, tokens, and sampled business data rows.
