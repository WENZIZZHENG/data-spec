## ADDED Requirements

### Requirement: Stable-reference and post-check fixtures
CLI/MCP contract fixtures SHALL cover standard reference resolution and AI output post-check entry points.

#### Scenario: Fixtures describe new CLI and MCP contracts
- **WHEN** fixture validation runs
- **THEN** fixtures SHALL include CLI command shape, exit codes, MCP tool descriptors, input schema descriptions, output shape, safety metadata, success examples, failure examples, and recommended next actions for resolve and post-check.

#### Scenario: Fixture checker detects drift
- **WHEN** CLI/MCP descriptors remove or rename stable fields, change PASS/WARN/FAIL semantics, weaken read-only safety, or expose secret-like examples
- **THEN** fixture validation SHALL fail with a readable diagnostic.
