## ADDED Requirements

### Requirement: AI Context Field Usage Contract Export
The AI Context package SHALL export field usage contracts in machine-readable and human-readable forms.

#### Scenario: Field catalog exports usage contract
- **WHEN** a field has one or more usage contract values
- **THEN** `.dataspec/field-catalog.json` includes a `usageContract` object for that field
- **AND** that object can include preferred use cases, avoid conditions, join hints, default filters, aggregation hints, replacement guidance, and misuse examples
- **AND** `.dataspec/field-catalog.schema.json` describes each usage contract property

#### Scenario: Database rules mention usage boundaries
- **WHEN** `DATABASE_RULES.md` is generated for fields with usage contracts
- **THEN** it includes concise field usage guidance for high-risk or scoped fields
- **AND** it tells AI clients to respect avoid conditions before generating SQL or DDL

#### Scenario: No usage contracts remain compatible
- **WHEN** a project has no field usage contract values
- **THEN** AI Context files remain valid
- **AND** empty usage contract sections are omitted rather than emitted as noisy placeholders
