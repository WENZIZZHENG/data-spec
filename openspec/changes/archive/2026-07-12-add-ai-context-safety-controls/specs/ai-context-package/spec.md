## ADDED Requirements

### Requirement: AI Context package includes safety controls
The AI Context package SHALL include additive safety controls without removing existing package files or field catalog fields.

#### Scenario: Package manifest records safety controls
- **WHEN** the AI Context package is generated
- **THEN** `.dataspec/manifest.json` SHALL include `contextSafetySummary`
- **AND** existing `schemaVersion`, `kind`, `projectId`, `standard`, `files`, `contracts`, and `commands` fields SHALL remain present.

#### Scenario: Field catalog schema describes safety fields
- **WHEN** `.dataspec/field-catalog.schema.json` is generated
- **THEN** it SHALL describe `contextSafety` and `exportDecision` for each field
- **AND** the descriptions SHALL state trust boundary, visibility, masking, allowed tasks, warning, and redaction semantics.

#### Scenario: Package guidance preserves instruction boundary
- **WHEN** AI clients read package guidance files
- **THEN** DataSpec instructions and schema registry contracts SHALL be described as trusted guidance
- **AND** business text inside fields, examples, SQL, glossary, metadata, and user descriptions SHALL be described as untrusted content.
