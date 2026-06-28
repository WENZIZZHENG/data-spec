## ADDED Requirements

### Requirement: Contract fixtures cover schema registry
The existing AI contract fixture checks SHALL cover the schema registry and registry consumers.

#### Scenario: Registry contract is incomplete
- **WHEN** a required core contract id, `schemaVersion`, `stableFields`, `jsonSchema`, or compatibility policy is missing
- **THEN** backend contract tests fail with a readable assertion.

#### Scenario: AI Context registry metadata drifts
- **WHEN** AI Context package manifest or `.dataspec/schema-registry.json` loses registry metadata
- **THEN** backend AI Context tests fail.

#### Scenario: CLI and MCP registry output drifts
- **WHEN** CLI contract commands or MCP schema registry resource lose stable registry fields
- **THEN** Node contract tests fail.
