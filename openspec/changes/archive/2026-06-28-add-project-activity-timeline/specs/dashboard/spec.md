## MODIFIED Requirements

### Requirement: Dashboard Page

DataSpec SHALL expose a Dashboard page as the default entry and include a project activity timeline for the current project.

#### Scenario: Open app root

- **WHEN** a user opens the app root route
- **THEN** the app navigates to Dashboard
- **AND** Dashboard refreshes when the current project changes.

#### Scenario: Show project activity timeline

- **WHEN** Dashboard has a current project
- **THEN** Dashboard loads and displays recent project activities
- **AND** the user can filter activities by action type.

#### Scenario: Jump to activity detail

- **WHEN** an activity contains a detailRoute
- **THEN** Dashboard provides an action that navigates to that route.
