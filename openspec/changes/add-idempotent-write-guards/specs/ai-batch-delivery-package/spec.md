## ADDED Requirements

### Requirement: AI batch write idempotency
AI batch SQL lint creation SHALL support idempotency keys and project-operation locking.

#### Scenario: Retry AI batch with same key
- **WHEN** a caller retries the same batch SQL lint request with the same idempotency key
- **THEN** DataSpec returns the original delivery package without creating another batch run record.

#### Scenario: Concurrent AI batch for same project
- **WHEN** a batch SQL lint request is already running for the project
- **THEN** another batch request for the same project and operation receives a retryable conflict diagnostic unless it can reuse a completed idempotency result.
