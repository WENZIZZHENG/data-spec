# idempotent-write-guards Specification

## Purpose
TBD - created by archiving change add-idempotent-write-guards. Update Purpose after archive.
## Requirements
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

### Requirement: Protected writes can require idempotency keys
DataSpec SHALL let protected high-risk writes require an idempotency key when their AI write safety metadata marks it as mandatory.

#### Scenario: Required key is missing
- **WHEN** a protected high-risk write declares `requiresIdempotencyKey=true`
- **AND** the caller omits `Idempotency-Key` or an equivalent idempotency parameter
- **THEN** DataSpec rejects the write before executing the protected action
- **AND** returns a structured safety diagnostic telling the caller to retry with an idempotency key.

#### Scenario: Optional key remains optional
- **WHEN** a protected write does not declare `requiresIdempotencyKey=true`
- **THEN** DataSpec preserves the existing operation lock behavior
- **AND** it does not require a key only because the write is protected.

#### Scenario: Required key uses existing reuse behavior
- **WHEN** a required idempotency key is present and the same project, operation, and key are submitted again during the backend process lifetime
- **THEN** DataSpec reuses the existing completed result according to the current idempotency guard behavior.

#### Scenario: Missing key diagnostic is redacted
- **WHEN** DataSpec returns the missing idempotency key diagnostic
- **THEN** the diagnostic contains no raw token, password, Authorization header, API key, complete JDBC URL, DSN, connection string, or source database business rows.

