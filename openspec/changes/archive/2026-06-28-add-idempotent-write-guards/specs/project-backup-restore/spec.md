## ADDED Requirements

### Requirement: Restore apply write idempotency
Project restore apply SHALL use the project-scoped write guard for target project mutations.

#### Scenario: Retry restore apply with same key
- **WHEN** a caller retries the same restore apply request with the same target project and idempotency key
- **THEN** DataSpec returns the original restore result without applying the restore twice.

#### Scenario: Concurrent restore apply
- **WHEN** a restore apply is already mutating the same target project
- **THEN** DataSpec returns a retryable conflict diagnostic for another restore apply on that target project.
