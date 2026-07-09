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

### Requirement: Frontend shows resumable AI tasks
DataSpec Web SHALL surface recent failed or resumable AI tasks from project task entrypoints.

#### Scenario: Dashboard shows recent failed AI tasks
- **WHEN** a project has failed or partially failed retryable task runs
- **THEN** the dashboard or task entrypoint area shows task type, failed step, retryable state, next action, and a way to copy the resume command.

#### Scenario: AI batch page shows linked task run
- **WHEN** a user views an AI batch run with linked task run metadata
- **THEN** the page shows task status, failed step, retryable state, partial artifacts, and resume command when available.

#### Scenario: No project selected
- **WHEN** no project is selected
- **THEN** the frontend does not call project-scoped task run APIs and shows the existing project-required state.

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

### Requirement: Frontend Task Card Display
DataSpec Web SHALL provide a lightweight task card display for project task entrypoints and AI handoff views.

#### Scenario: Show task card summary
- **WHEN** the frontend receives a valid task card object
- **THEN** it displays goal, status, current step, next command, validation commands, artifacts, risks, and stop conditions.

#### Scenario: Missing or invalid task card
- **WHEN** the task card object is missing or invalid
- **THEN** the frontend shows a non-sensitive empty or invalid state instead of rendering raw JSON errors.

#### Scenario: Copy task card markdown
- **WHEN** a user copies a task card for handoff
- **THEN** the frontend provides Markdown containing the same non-sensitive summary fields.

### Requirement: Frontend exposes maintenance workflow dry-run entrypoints
DataSpec Web SHALL expose standard maintenance workflow dry-run actions from high-frequency maintenance pages.

#### Scenario: Candidate page opens workflow plan
- **WHEN** a user opens the standard candidate workbench with a current project
- **THEN** the page offers a dry-run maintenance workflow action for pending or postponed candidates
- **AND** the resulting plan view shows steps, evidence links, confirmation requirements, and verification guidance.

#### Scenario: Quality and coverage pages open workflow plan
- **WHEN** a user views field quality issues or field coverage unmanaged findings with a current project
- **THEN** the page offers a dry-run maintenance workflow action for the current filtered or selected findings
- **AND** partial, failed, empty, and missing-project states remain recoverable and non-sensitive.

#### Scenario: Frontend does not execute hidden writes
- **WHEN** a user generates or views a maintenance workflow plan
- **THEN** DataSpec Web calls only the plan API for dry-run generation
- **AND** write actions remain separate explicit actions through existing candidate or field maintenance flows.
