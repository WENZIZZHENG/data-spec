## MODIFIED Requirements

### Requirement: Field Catalog Schema
The AI context package SHALL include a JSON schema that describes field metadata.

#### Scenario: Schema contains personal field metadata
- **WHEN** the AI context package is generated
- **THEN** `.dataspec/field-catalog.schema.json` allows field aliases, category, codeSetId, sensitive, status, and example
