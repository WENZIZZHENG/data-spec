## MODIFIED Requirements

### Requirement: Local Compose Startup

DataSpec SHALL provide a reproducible local startup package that runs PostgreSQL, the backend service, and the web frontend with local-development defaults.

#### Scenario: Start a healthy local stack

- **WHEN** a user runs the documented detached Compose command with wait enabled
- **THEN** PostgreSQL, backend, and frontend start with health checks
- **AND** the command completes only after all three services are healthy
- **AND** the backend uses Flyway migrations against the local PostgreSQL service.

#### Scenario: Keep insecure development defaults local

- **WHEN** the Compose stack runs with local authentication disabled and development database credentials
- **THEN** published PostgreSQL, backend, and frontend ports bind to the loopback interface by default
- **AND** non-loopback binding requires explicit bind-host and security environment overrides.

#### Scenario: Isolate parallel worktrees

- **WHEN** local stacks are started from different worktree directories
- **THEN** Compose does not force them to share one fixed project name or database volume
- **AND** users can explicitly set `COMPOSE_PROJECT_NAME` when they intentionally want a stable local project identity.

#### Scenario: Avoid the common local PostgreSQL port conflict

- **WHEN** the user starts Compose without a database port override
- **THEN** PostgreSQL is published on host port `15432`
- **AND** container-to-container database access continues to use port `5432`.

#### Scenario: Override local ports

- **WHEN** default ports conflict with another local service
- **THEN** the startup package allows overriding the bind host and exposed PostgreSQL, backend, or frontend ports through environment variables.

#### Scenario: Install frontend dependencies reproducibly

- **WHEN** the local Compose frontend starts with an empty dependency volume and package-manager supply-chain verification enabled
- **THEN** it uses the repository-declared Node and pnpm toolchain versions
- **AND** the frozen frontend lockfile passes verification without disabling or relaxing the package-manager policy
- **AND** the locked dependency set remains unchanged
- **AND** the frontend development server starts successfully.

### Requirement: Demo Smoke Verification

DataSpec SHALL provide a bounded local smoke verification command for the demo stack.

#### Scenario: Verify healthy demo stack

- **WHEN** PostgreSQL, backend, and frontend are running
- **THEN** the smoke command waits for the web frontend and API docs
- **AND** creates or reuses the demo project
- **AND** verifies dashboard summary and SQL lint can run against the demo project.

#### Scenario: Stop a hanging request at the configured timeout

- **WHEN** a target accepts an HTTP connection but does not return a response before the configured timeout
- **THEN** the smoke command aborts the request and exits non-zero within the bounded timeout
- **AND** reports the failed check without exposing credentials.

#### Scenario: Emit machine readable result

- **WHEN** the smoke command is run with JSON output
- **THEN** it prints a JSON object containing ok, server, web, projectId, and checks
- **AND** each check contains name, status, and message.

#### Scenario: Diagnose unavailable service

- **WHEN** the backend or frontend cannot be reached before the timeout
- **THEN** the smoke command exits non-zero
- **AND** prints the failed check and a next action.
