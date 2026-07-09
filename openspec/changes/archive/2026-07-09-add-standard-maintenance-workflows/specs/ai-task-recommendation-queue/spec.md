## ADDED Requirements

### Requirement: AI recommendations bind to maintenance workflow recipes
AI task recommendations SHALL be able to bind standard maintenance recommendations to a stable workflow recipe and completion check.

#### Scenario: Candidate recommendation includes recipe binding
- **WHEN** the recommendation queue includes candidate, field quality, coverage, or quality gate maintenance work
- **THEN** each relevant recommendation includes a `recipeBinding` for `standard-maintenance`, safe source parameters, evidence references, and a completion check.

#### Scenario: Recommendation remains read-only
- **WHEN** recommendations include maintenance workflow bindings
- **THEN** DataSpec still returns only routes, dry-run commands, recipe metadata, and completion checks
- **AND** it does not execute the workflow or mutate candidate, field, quality, or coverage data.
