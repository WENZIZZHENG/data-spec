## ADDED Requirements

### Requirement: Registry covers test data and compatibility contracts
Schema Registry SHALL register the standard test data package and consumer compatibility suite contracts for AI and local validation consumers.

#### Scenario: Catalog lists new contracts
- **WHEN** a client requests the schema registry catalog
- **THEN** the catalog includes contract summaries for `standard-test-data-package`, `consumer-compatibility-suite`, `consumer-compatibility-adapter-result`, and `consumer-compatibility-breaking-rule`
- **AND** each summary includes schema version, stable fields, deprecated fields, JSON Schema reference, docs reference, and compatibility policy.

#### Scenario: Contract details include safety descriptions
- **WHEN** a client requests a test data or compatibility contract detail
- **THEN** the detail includes JSON Schema descriptions for safety metadata, generated examples, adapter results, diagnostics, stable fields, deprecated fields, and next actions
- **AND** secret-like example values are redacted or represented as placeholders.
