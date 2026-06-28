## ADDED Requirements

### Requirement: Unified State Smoke Coverage
The frontend smoke regression gate SHALL cover the unified request state and project-required entry points for migrated pages.

#### Scenario: State helpers are removed
- **WHEN** a developer removes the unified request state utility or state display components while migrated pages still depend on them
- **THEN** the frontend smoke test fails with a readable assertion.

#### Scenario: Migrated page loses retry entry
- **WHEN** a migrated page no longer exposes a visible retry or recovery action for failed requests
- **THEN** the frontend smoke test fails.
