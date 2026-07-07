## ADDED Requirements

### Requirement: Budget-aware AI Context export preview
AI Context scoped export SHALL expose budget planning as a pre-export preview without changing existing export semantics.

#### Scenario: Preview budget before export
- **WHEN** a frontend or API caller prepares an AI Context export with scope, query, status, limit, profile, task type, or token budget
- **THEN** DataSpec can return a budget plan using the same effective scope semantics as scoped export.
- **AND** the existing field catalog preview, package download, and cache export behavior remain compatible when no budget plan is requested.

#### Scenario: Recommended export params remain advisory
- **WHEN** the budget plan returns `recommendedExportParams`
- **THEN** the frontend may show or apply those parameters only as an explicit user action.
- **AND** DataSpec MUST NOT silently override existing export parameters solely because the planner produced a recommendation.
