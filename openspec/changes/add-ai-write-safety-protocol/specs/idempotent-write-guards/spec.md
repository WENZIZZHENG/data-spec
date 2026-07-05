## ADDED Requirements

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
