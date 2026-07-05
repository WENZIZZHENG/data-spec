## ADDED Requirements

### Requirement: Workflow Recipes Seed Task Cards
Workflow recipes SHALL provide enough structured metadata to seed an AI task card.

#### Scenario: Generate card from recipe
- **WHEN** a caller creates a task card from a known workflow recipe
- **THEN** DataSpec maps recipe inputs, ordered steps, expected artifacts, failure handling, and next actions into the task card.

#### Scenario: Unknown workflow recipe
- **WHEN** a caller asks for a task card from an unknown workflow recipe
- **THEN** DataSpec returns a parameter error that includes supported recipe ids.
