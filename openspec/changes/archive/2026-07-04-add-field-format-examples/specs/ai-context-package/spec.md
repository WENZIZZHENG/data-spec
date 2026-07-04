## MODIFIED Requirements

### Requirement: Machine Readable Field Catalog Contract
The AI Context package SHALL include a JSON Schema describing the generated field catalog.

#### Scenario: Schema matches generated catalog
- **WHEN** the package is generated
- **THEN** `.dataspec/field-catalog.schema.json` defines the top-level `projectId`, `fields`, and `enums` properties
- **AND** `.dataspec/field-catalog.json` remains valid JSON using the same top-level property names

#### Scenario: Schema contains personal field metadata
- **WHEN** the AI Context package is generated
- **THEN** `.dataspec/field-catalog.schema.json` allows field aliases, category, codeSetId, sensitive, status, and example

#### Scenario: Schema contains field format constraints
- **WHEN** the AI Context package is generated
- **THEN** `.dataspec/field-catalog.schema.json` allows each field to include a `format` object with type, pattern, unit, precision, timezone, nullPolicy, validExamples, invalidExamples, and notes.
- **AND** `validExamples` and `invalidExamples` are arrays of strings.

## ADDED Requirements

### Requirement: AI Context Field Format Export
The AI Context package SHALL export field value-format constraints in AI-readable context.

#### Scenario: Field catalog exports format constraints
- **WHEN** a field has format constraints
- **THEN** `.dataspec/field-catalog.json` SHALL include a `format` object for that field.
- **AND** the object SHALL preserve unit, timezone, precision, valid examples, invalid examples, and notes without exposing business data rows.

#### Scenario: Database rules mention format constraints
- **WHEN** `DATABASE_RULES.md` is generated
- **THEN** fields with format constraints SHALL include a concise value-format line or inline summary.
- **AND** create-table/fix-sql prompts that embed AI Context SHALL allow the AI to read these constraints before generating DDL or SQL fixes.
