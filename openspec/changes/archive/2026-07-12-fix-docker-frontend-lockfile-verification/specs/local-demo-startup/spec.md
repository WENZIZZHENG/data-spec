## MODIFIED Requirements

### Requirement: Local Compose Startup

DataSpec SHALL provide a local startup package that can run PostgreSQL, the backend service, and the web frontend with development defaults.

#### Scenario: Start local stack

- **WHEN** a user runs the documented local compose command
- **THEN** DataSpec starts PostgreSQL, backend, and frontend services with default development ports
- **AND** the backend uses Flyway migrations against the local PostgreSQL service.

#### Scenario: Install frontend dependencies in a clean container

- **WHEN** the local Compose frontend starts with an empty dependency volume and package-manager supply-chain verification enabled
- **THEN** the frozen frontend lockfile passes verification without disabling or relaxing the package-manager policy
- **AND** the frontend development server starts from the locked dependency set.

#### Scenario: Override local ports

- **WHEN** default ports conflict with another local service
- **THEN** the startup package allows overriding exposed PostgreSQL, backend, or frontend ports through environment variables.
