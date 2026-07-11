## ADDED Requirements

### Requirement: AI Context Field Semantics Export
The AI Context package SHALL export field semantics, knowledge cards, enum lifecycle, naming guidance, and metric mappings in bounded, secret-safe artifacts.

#### Scenario: Package contains field knowledge cards artifact
- **WHEN** an AI Context zip is generated for a project
- **THEN** it contains a field knowledge card artifact with projectId, schemaVersion, contextScope, cards, and summary
- **AND** manifest files list the artifact and report truncation when cards are bounded.

#### Scenario: Package contains field semantics artifact
- **WHEN** a project has field semantic rules or naming translation guidance
- **THEN** AI Context includes an artifact that lists semantic rules, derived relationships, unit conversion notes, time granularity, source-of-truth guidance, preferred names, forbidden translations, and anti-patterns
- **AND** empty projects remain compatible with valid empty arrays or omitted optional sections.

#### Scenario: Package contains metric mapping artifact
- **WHEN** a project has metric definitions
- **THEN** AI Context includes a metric mapping artifact with metricKey, definition, field refs, filter rule, aggregation rule, time grain, example SQL summary, and evidence refs
- **AND** the artifact is marked as metadata guidance, not executable SQL.

#### Scenario: Package enum output carries lifecycle
- **WHEN** AI Context exports enums
- **THEN** enum values include optional lifecycle status, aliases, replacement value, validity window, and mapping hints
- **AND** existing enum value and label fields remain present.

#### Scenario: Database rules mention semantic guardrails
- **WHEN** `DATABASE_RULES.md` is generated
- **THEN** it includes concise guidance for high-risk unit conversion, source-of-truth, enum lifecycle, forbidden translation, and metric-boundary cases
- **AND** it tells AI clients to open the detailed artifacts before generating DDL, SQL, mocks, or tests that depend on those semantics.
