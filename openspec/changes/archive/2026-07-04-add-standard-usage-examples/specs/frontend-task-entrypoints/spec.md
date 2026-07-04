## ADDED Requirements

### Requirement: Frontend Standard Usage Example Entry
DataSpec Web SHALL expose a project-scoped maintenance entry for standard usage examples and anti-examples.

#### Scenario: Navigate to usage examples
- **WHEN** a user opens standard maintenance task entry points with a current project
- **THEN** the frontend offers a route to the standard usage example library.

#### Scenario: Manage examples
- **WHEN** a user opens the usage example library with a current project
- **THEN** the page lists examples with scope, example type, target, priority, status, tags, and reason
- **AND** the user can create, edit, and delete examples without leaving the page.

#### Scenario: No project selected
- **WHEN** no project is selected
- **THEN** the usage example library does not call project-scoped APIs
- **AND** it shows the existing shared project-required state.
