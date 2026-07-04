## ADDED Requirements

### Requirement: Frontend shows quality gate status
DataSpec Web SHALL surface project quality gate status from standard maintenance entry points.

#### Scenario: Show gate status
- **WHEN** a user opens the standard health or field quality area with a current project selected
- **THEN** the page shows whether the quality gate is disabled, passing, or failing, including failed checks and next actions.

#### Scenario: Navigate from failed gate check
- **WHEN** a gate check fails because of field quality, coverage, lint, unmanaged fields, or sensitive marking
- **THEN** the frontend provides a route or action to the relevant repair page without blocking local editing.

#### Scenario: No project selected
- **WHEN** no project is selected
- **THEN** the frontend does not call project-scoped quality gate APIs and shows the existing project-required state.
