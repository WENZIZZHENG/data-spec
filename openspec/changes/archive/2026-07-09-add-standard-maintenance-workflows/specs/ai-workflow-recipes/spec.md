## ADDED Requirements

### Requirement: Publish standard maintenance workflow recipe
The workflow recipe catalog SHALL include a `standard-maintenance` recipe for turning DataSpec maintenance signals into a safe dry-run task plan.

#### Scenario: Recipe catalog includes standard maintenance
- **WHEN** an AI agent or user lists workflow recipes through CLI, MCP resource, or AI Context package
- **THEN** the catalog includes `standard-maintenance` with required inputs, prechecks, ordered steps, expected artifacts, failure handling, and next actions.

#### Scenario: Recipe is plan-only
- **WHEN** a caller inspects the `standard-maintenance` recipe
- **THEN** the recipe describes the plan API, existing confirmation APIs or pages, verification checks, evidence capture, and stop conditions
- **AND** it does not claim to automatically accept candidates, edit fields, run background jobs, or call external LLMs.
