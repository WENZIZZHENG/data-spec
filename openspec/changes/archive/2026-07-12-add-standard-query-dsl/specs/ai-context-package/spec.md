## ADDED Requirements

### Requirement: AI Context export accepts Standard Query DSL scope
AI Context export SHALL support Standard Query DSL as an additive field catalog and package scope.

#### Scenario: Export field catalog with DSL
- **WHEN** a caller requests AI Context field catalog or package export with a Standard Query DSL
- **THEN** the generated field catalog contains only fields that match the DSL execution result
- **AND** the manifest or context metadata records a redacted query summary, applied filters, ignored filters, and result counts.

#### Scenario: Legacy scope remains compatible
- **WHEN** a caller uses existing scope, query, status, limit, profileId, or taskType parameters
- **THEN** AI Context export continues to work
- **AND** DataSpec may map compatible legacy scope parameters into the same DSL summary.
