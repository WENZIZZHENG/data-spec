## ADDED Requirements

### Requirement: Registry includes field semantics contracts
The schema registry SHALL describe the AI-consumed contracts introduced for field semantics and knowledge cards.

#### Scenario: Registry lists new semantic contracts
- **WHEN** a client lists the schema registry catalog
- **THEN** it includes contracts for FieldSemanticRule, FieldKnowledgeCard, EnumValueLifecycle, MetricDefinitionMapping, and AI Context field semantics artifacts
- **AND** each contract includes schemaVersion, stable fields, docsRef, jsonSchemaRef, compatibility policy, and examples when available.

#### Scenario: Contract detail describes secret-safe fields
- **WHEN** a client shows a semantic contract detail
- **THEN** text fields that can contain user-maintained guidance include descriptions requiring secret-safe content and no sampled business rows.

#### Scenario: Additive compatibility is explicit
- **WHEN** existing Field, Enum, AI Context field catalog, or data dictionary contracts gain optional semantic fields
- **THEN** the registry marks the additions as compatible and documents stable field paths for AI clients.
