## ADDED Requirements

### Requirement: AI Context Package Includes Usage Examples
The AI Context package SHALL include concise standard usage examples and anti-examples for AI clients.

#### Scenario: Package contains usage examples file
- **WHEN** an AI Context zip is generated for a project
- **THEN** it contains `.dataspec/usage-examples.json`
- **AND** that file contains `projectId`, `schemaVersion`, `contextScope`, `examples`, and `summary`.

#### Scenario: Field catalog references usage examples
- **WHEN** `field-catalog.json` is generated
- **THEN** the top-level JSON includes additive `usageExamples` and `usageExampleSummary` properties
- **AND** existing `projectId`, `standard`, `fields`, and `enums` properties remain present.

#### Scenario: Package guidance mentions examples
- **WHEN** a caller reads `.dataspec/README.md` or `AGENTS.md.fragment`
- **THEN** the guidance tells AI clients to prefer `GOOD` examples and avoid `BAD` examples with matching scope.

#### Scenario: No examples remain compatible
- **WHEN** a project has no enabled usage examples
- **THEN** the package still contains a valid `.dataspec/usage-examples.json` with an empty `examples` array
- **AND** existing package files remain generated.
