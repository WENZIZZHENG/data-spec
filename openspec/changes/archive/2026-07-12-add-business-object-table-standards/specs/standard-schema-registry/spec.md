## ADDED Requirements

### Requirement: Registry contains table standard contracts
The schema registry SHALL register the contracts used by business object and table structure standards.

#### Scenario: Catalog lists table standard contracts
- **WHEN** the schema registry catalog is generated
- **THEN** it contains contract summaries for BusinessObjectStandard, TableStructureStandard, TableRelationHint, TableIndexStandard, TableForeignKeyStandard, TablePolicyStandard, and AI Context table standards
- **AND** every summary includes schema version, stable fields, JSON Schema reference, docs reference, and compatibility policy.

#### Scenario: Contract detail describes stable fields
- **WHEN** a client requests one of the table standard contract details
- **THEN** the response describes each stable field, nested object, array shape, nullability, allowed enum values, and redaction boundary
- **AND** unknown contract ids continue to return the existing readable failure with supported ids.

### Requirement: Registry compatibility for additive table standards
The schema registry SHALL mark table standard additions as compatible when existing stable fields are preserved.

#### Scenario: Add optional table-standard fields
- **WHEN** business object, table structure, DDL preview, or AI Context contracts gain optional table-standard fields
- **THEN** the registry compatibility policy marks the change as additive
- **AND** it documents that removing or renaming stable table-standard fields requires a schema version bump and migration note.
