# local-demo-startup Specification

## Purpose
Defines the local startup and smoke verification contract that lets DataSpec users and AI agents bring up PostgreSQL, the backend, and the web frontend with development defaults, then verify the demo project, dashboard, and SQL lint links with a repeatable command.
## Requirements
### Requirement: Local Compose Startup

DataSpec SHALL provide a local startup package that can run PostgreSQL, the backend service, and the web frontend with development defaults.

#### Scenario: Start local stack

- **WHEN** a user runs the documented local compose command
- **THEN** DataSpec starts PostgreSQL, backend, and frontend services with default development ports
- **AND** the backend uses Flyway migrations against the local PostgreSQL service.

#### Scenario: Override local ports

- **WHEN** default ports conflict with another local service
- **THEN** the startup package allows overriding exposed PostgreSQL, backend, or frontend ports through environment variables.

### Requirement: Demo Smoke Verification

DataSpec SHALL provide a local smoke verification command for the demo stack.

#### Scenario: Verify healthy demo stack

- **WHEN** PostgreSQL, backend, and frontend are running
- **THEN** the smoke command waits for the web frontend and API docs
- **AND** creates or reuses the demo project
- **AND** verifies dashboard summary and SQL lint can run against the demo project.

#### Scenario: Emit machine readable result

- **WHEN** the smoke command is run with JSON output
- **THEN** it prints a JSON object containing ok, server, web, projectId, and checks
- **AND** each check contains name, status, and message.

#### Scenario: Diagnose unavailable service

- **WHEN** the backend or frontend cannot be reached before the timeout
- **THEN** the smoke command exits non-zero
- **AND** prints the failed check and a next action.

### Requirement: Local Startup Documentation

DataSpec SHALL document the difference between one-command local startup and manual development startup.

#### Scenario: Read local startup instructions

- **WHEN** a user reads README startup instructions
- **THEN** they can find commands for compose startup, smoke verification, manual backend/frontend development mode, and local cleanup.

#### Scenario: Explain local-only boundary

- **WHEN** a user reads the local startup documentation
- **THEN** DataSpec explains that the compose defaults are for personal local development and are not a production deployment recipe.
