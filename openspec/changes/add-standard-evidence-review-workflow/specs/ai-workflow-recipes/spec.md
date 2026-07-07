## MODIFIED Requirements

### Requirement: Publish AI workflow recipes
The system SHALL provide workflow recipes that describe common AI/DataSpec tasks with inputs, ordered steps, commands, expected outputs, failure handling, and next actions.

#### Scenario: List supported recipes
- **WHEN** an AI agent or user asks for workflow recipes through CLI or MCP
- **THEN** the system returns a stable catalog that includes at least create-table, review-pr-sql, reverse-import-standards, export-min-context, and standard-evidence-review recipes.

#### Scenario: Inspect a recipe
- **WHEN** an AI agent requests a specific recipe
- **THEN** the system returns the recipe id, title, goal, required inputs, prechecks, ordered steps, expected artifacts, failure handling, and next actions.

#### Scenario: Inspect standard evidence review recipe
- **WHEN** an AI agent requests the standard-evidence-review recipe
- **THEN** the recipe describes a read-only, plan-only workflow that uses projectId, subjectType, and subjectId to read `GET /api/standard-evidence`
- **AND** it does not claim a dedicated CLI command, MCP tool, or MCP resource for standard evidence.
