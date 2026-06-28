## ADDED Requirements

### Requirement: Task Entry Points

DataSpec Web SHALL expose task-oriented entry points for common personal and AI-assisted workflows.

#### Scenario: Show task entry points

- **WHEN** a user opens Dashboard with a current project
- **THEN** DataSpec shows task entry points for database reverse import, SQL lint, field coverage, field maintenance, AI Context export, and API Token management.

#### Scenario: Open task entry point

- **WHEN** a user activates a task entry point
- **THEN** DataSpec navigates to the matching existing route.

### Requirement: Recent Task Memory

DataSpec Web SHALL remember recently used task entry points in the current browser without sending that preference to the backend.

#### Scenario: Record recent task

- **WHEN** a user opens a task entry point from Dashboard
- **THEN** DataSpec stores the task key, route, projectId, title, and usedAt in localStorage.

#### Scenario: Show recent project tasks

- **WHEN** Dashboard has a current project and recent tasks for that project
- **THEN** DataSpec shows the most recent task entries for the project.

#### Scenario: Tolerate damaged recent task storage

- **WHEN** localStorage contains invalid recent task data
- **THEN** Dashboard clears the damaged value and continues rendering.

### Requirement: Breadcrumb Navigation Context

DataSpec Web SHALL provide lightweight breadcrumb context based on the current route.

#### Scenario: Show current page breadcrumb

- **WHEN** a user navigates to a routed page
- **THEN** App header shows Dashboard and the current route title as breadcrumb items.
