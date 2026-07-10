## ADDED Requirements

### Requirement: Registry describes stable references and post-check results
The standard schema registry SHALL describe the stable-reference and AI output post-check contracts.

#### Scenario: Registry catalog includes new contracts
- **WHEN** the registry catalog is generated
- **THEN** it SHALL include contract summaries for standard reference resolution and AI output post-check results.

#### Scenario: Contract detail describes stable fields
- **WHEN** a caller requests either new contract detail
- **THEN** the JSON Schema SHALL describe required fields, enums, array item semantics, compatibility policy, secret-safety constraints, and examples
- **AND** additive stableRef fields on existing contracts SHALL be documented.
