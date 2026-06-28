## ADDED Requirements

### Requirement: Recoverable Task Page States
DataSpec Web task entry pages SHALL provide consistent recoverable states when project selection or backend requests block a workflow.

#### Scenario: Task page missing project
- **WHEN** a user opens a project-scoped task page without a selected project
- **THEN** the page shows the shared project-required state instead of a page-specific raw empty state.

#### Scenario: Task page request fails
- **WHEN** a migrated task page fails to load project-scoped data
- **THEN** the page shows a shared failed state with retry and non-sensitive suggested action text.
