## ADDED Requirements

### Requirement: Field Semantic Rules
DataSpec SHALL store project-scoped semantic rules that explain how standard fields relate to source fields, derived fields, units, time grains, aggregation rules, naming translations, and source-of-truth guidance.

#### Scenario: Create field semantic rule
- **WHEN** a caller creates a semantic rule for a standard field with `projectId`, `fieldId`, optional `sourceFieldId`, `ruleType`, `unitConversion`, `aggregationRule`, `timeGranularity`, `sourceOfTruth`, `recommendedUse`, and `antiPatterns`
- **THEN** DataSpec persists the rule under the same project
- **AND** cross-field references MUST belong to the same project
- **AND** secret-like text, raw JDBC URLs, DSNs, Authorization headers, tokens, passwords, and sampled business rows MUST be rejected.

#### Scenario: List field semantic rules
- **WHEN** a caller lists semantic rules by `projectId`, `fieldId`, `ruleType`, or `query`
- **THEN** DataSpec returns only non-deleted rules from the requested project
- **AND** the result includes enough field identity and stable reference metadata for AI clients to explain the relationship.

### Requirement: Field Naming Translation Guidance
DataSpec SHALL expose project-maintained naming translation guidance for standard fields.

#### Scenario: Field carries naming translation guidance
- **WHEN** a field is created, updated, listed, searched, exported to AI Context, or serialized through OpenAPI
- **THEN** it can include localized names, preferred English name, translation aliases, forbidden translations, translation confidence, and translation notes
- **AND** existing field clients remain compatible when these optional values are absent.

#### Scenario: Forbidden translation explains fallback
- **WHEN** a search or suggestion query matches a forbidden translation
- **THEN** DataSpec includes an AI-readable warning or next action that tells the caller not to use that translation without confirmation.

### Requirement: Field Knowledge Cards
DataSpec SHALL provide read-only FieldKnowledgeCard views that aggregate field metadata and related semantic evidence for AI and frontend users.

#### Scenario: Show field knowledge card
- **WHEN** a caller requests a knowledge card for a valid field in a project
- **THEN** DataSpec returns the field identity, stableRef, lifecycle, format constraints, usage contract, semantic rules, naming guidance, enum hints, usage examples, related fields, metric references, risk notes, evidence refs, and lastVerifiedAt when available
- **AND** generated text is bounded and secret-safe.

#### Scenario: Knowledge card remains valid with sparse metadata
- **WHEN** a field has no semantic rules, examples, metrics, enum values, or naming guidance
- **THEN** DataSpec still returns a valid knowledge card with empty arrays or omitted optional sections rather than failing.

#### Scenario: Scoped knowledge card export
- **WHEN** a caller lists knowledge cards with `projectId`, optional `query`, `status`, `fieldId`, or `limit`
- **THEN** DataSpec returns a bounded set ordered for AI usefulness
- **AND** the response reports truncation when the requested scope has more cards than returned.
