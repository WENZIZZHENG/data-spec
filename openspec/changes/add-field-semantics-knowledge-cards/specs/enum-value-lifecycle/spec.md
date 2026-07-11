## ADDED Requirements

### Requirement: Enum Value Lifecycle Metadata
DataSpec SHALL allow enum values to carry lifecycle and mapping metadata for AI-readable code set usage.

#### Scenario: Enum value stores lifecycle guidance
- **WHEN** a caller creates or updates an enum value with status, aliases, replacement value, validFrom, validTo, sourceEvidence, mappingHints, and AI usage notes
- **THEN** DataSpec persists and returns those optional values with the enum value
- **AND** valid statuses include `enabled`, `deprecated`, `disabled`, and `draft`.

#### Scenario: Deprecated enum value references replacement
- **WHEN** an enum value is deprecated or disabled with a replacement value
- **THEN** DataSpec returns the replacement guidance in enum detail, field knowledge cards, AI Context, and Schema Registry examples
- **AND** callers can distinguish current allowed values from deprecated or disabled values.

### Requirement: Enum Lifecycle AI Consumption
DataSpec SHALL expose enum lifecycle metadata to AI clients without requiring direct database inspection.

#### Scenario: Field knowledge card includes enum lifecycle
- **WHEN** a field references a code set through `codeSetId`
- **THEN** the field knowledge card includes current enum values, deprecated values, aliases, replacement hints, and mapping notes for that code set.

#### Scenario: AI Context exports enum lifecycle
- **WHEN** AI Context exports field catalog or enum lifecycle artifacts
- **THEN** enum values include lifecycle status, display label, aliases, replacement value, validity window, and mapping hints when available
- **AND** existing top-level `enums` output remains compatible for clients that only read value and label.

### Requirement: Enum Literal Guidance
DataSpec SHALL provide deterministic guidance when a SQL or AI workflow references enum literals for fields with known code sets.

#### Scenario: Illegal enum literal is explainable
- **WHEN** DataSpec can associate a checked field with a code set and the input literal is not an enabled enum value or alias
- **THEN** it returns a non-destructive warning with allowed values and replacement guidance
- **AND** it MUST NOT automatically rewrite production SQL.
