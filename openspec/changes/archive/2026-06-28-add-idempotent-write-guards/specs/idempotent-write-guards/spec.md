## ADDED Requirements

### Requirement: Project scoped write idempotency
DataSpec SHALL provide a lightweight single-instance idempotency guard for high-risk project-scoped writes.

#### Scenario: Duplicate key reuses successful result
- **WHEN** the same project, operation, and idempotency key are submitted more than once during the backend process lifetime
- **THEN** DataSpec returns the first successful result without executing the protected write again.

#### Scenario: Different operations keep separate scopes
- **WHEN** the same idempotency key is used for two different protected operations in the same project
- **THEN** each operation maintains its own result and lock scope.

### Requirement: Project operation lock
DataSpec SHALL protect high-risk project writes with a project and operation scoped lock.

#### Scenario: Concurrent write is already running
- **WHEN** a protected write for the same project and operation is already running
- **THEN** a second write without a reusable completed idempotency result fails with a retryable conflict diagnostic.

### Requirement: Protected write coverage
DataSpec SHALL apply the write guard to the first batch of high-risk AI/CLI write flows.

#### Scenario: First protected flows
- **WHEN** callers create a standard snapshot, confirm reverse import candidates, run AI batch SQL lint, apply a project restore, or record an AI job replay
- **THEN** the operation is covered by idempotency or stable fingerprint deduplication.
