## ADDED Requirements

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
