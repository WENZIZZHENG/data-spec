# frontend-task-entrypoints Specification

## Purpose
Defines the lightweight web entry points that help users and AI-assisted workflows start common DataSpec tasks from the Dashboard, remember recent task choices locally, and keep route context visible through breadcrumbs.
## Requirements
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

### Requirement: Evidence package front-end actions
DataSpec Web SHALL expose lightweight evidence package actions on high-frequency result views.

#### Scenario: SQL check evidence action
- **WHEN** a user views a SQL check result or record detail
- **THEN** the UI offers a way to copy evidence JSON or download an evidence zip for that SQL check.

#### Scenario: Coverage report evidence action
- **WHEN** a user views a field coverage report
- **THEN** the UI offers a way to copy evidence JSON or download an evidence zip for the current coverage report summary.

#### Scenario: AI batch evidence action
- **WHEN** a user views an AI batch run result or detail
- **THEN** the UI offers a way to copy evidence JSON or download an evidence zip for that batch run.

#### Scenario: Evidence action state
- **WHEN** no current project or source result is available
- **THEN** the evidence action is disabled or hidden with a clear non-sensitive state.

### Requirement: Recoverable Task Page States
DataSpec Web task entry pages SHALL provide consistent recoverable states when project selection or backend requests block a workflow.

#### Scenario: Task page missing project
- **WHEN** a user opens a project-scoped task page without a selected project
- **THEN** the page shows the shared project-required state instead of a page-specific raw empty state.

#### Scenario: Task page request fails
- **WHEN** a migrated task page fails to load project-scoped data
- **THEN** the page shows a shared failed state with retry and non-sensitive suggested action text.
