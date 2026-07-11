## ADDED Requirements

### Requirement: Field search accepts Standard Query DSL
Field standard search SHALL accept additive Standard Query DSL input without removing existing query and filter parameters.

#### Scenario: Search with DSL
- **WHEN** a caller searches fields with a Standard Query DSL targeting `FIELD`
- **THEN** the response contains the same field item shape as existing field search
- **AND** each returned item preserves field, score, matchReasons, recommendedUse, usageContractSummary, evidence, stableRef, canonicalRef, lifecycleStatus, and matchedAlias.

#### Scenario: Search summary includes DSL explanation
- **WHEN** a field search executes through DSL or legacy parameters mapped to DSL
- **THEN** the response summary includes querySummary, appliedFilters, ignoredFilters, resultCount, returnedCount, truncated, and nextQueryHints.

### Requirement: Field search DSL remains compatible
Existing field search clients SHALL remain compatible when DSL metadata is added.

#### Scenario: Existing GET field search still works
- **WHEN** a caller uses existing `/api/fields/search` query parameters
- **THEN** DataSpec returns the existing stable search result fields
- **AND** additive DSL explanation fields do not change previous field search semantics.
