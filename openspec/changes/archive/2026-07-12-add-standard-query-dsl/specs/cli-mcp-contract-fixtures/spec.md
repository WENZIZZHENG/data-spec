## ADDED Requirements

### Requirement: Standard Query DSL fixtures
CLI/MCP contract fixtures SHALL cover Standard Query DSL entry points and additive `search-fields` / `search_fields` DSL parameters.

#### Scenario: Fixtures describe DSL contracts
- **WHEN** fixture validation runs
- **THEN** fixtures include CLI command shape, MCP tool descriptors, input schema descriptions, output shape, safety metadata, success examples, failure examples, and recommended next actions for DSL queries.

#### Scenario: Fixture checker detects DSL drift
- **WHEN** CLI/MCP descriptors remove or rename DSL fields, weaken read-only safety, change supported filter semantics, or expose secret-like examples
- **THEN** fixture validation fails with a readable diagnostic.
