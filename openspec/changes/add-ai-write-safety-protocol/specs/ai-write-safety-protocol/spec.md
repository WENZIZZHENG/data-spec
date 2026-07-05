## ADDED Requirements

### Requirement: Unified AI write safety metadata
DataSpec SHALL define a machine-readable write safety metadata object for AI-consumed capability, CLI, MCP, API, and frontend outputs.

#### Scenario: Safety metadata shape
- **WHEN** DataSpec describes an AI-usable operation
- **THEN** the operation safety metadata includes `readOnly`, `writesProject`, `requiresDryRun`, `supportsUndo`, `requiresIdempotencyKey`, `sensitiveInputs`, and `nextActions`
- **AND** each field has stable machine-readable semantics.

#### Scenario: Read-only operation safety
- **WHEN** an operation does not mutate DataSpec state or external systems
- **THEN** `readOnly` is `true`
- **AND** `writesProject`, `requiresDryRun`, and `requiresIdempotencyKey` are `false`.

#### Scenario: Project write operation safety
- **WHEN** an operation writes project-scoped DataSpec records, standards, restore results, import decisions, or external review comments
- **THEN** `readOnly` is `false`
- **AND** `writesProject` is `true`
- **AND** `nextActions` tells AI clients which preview, dry-run, confirmation, idempotency, evidence, or recovery step to use before or after the write.

### Requirement: High-risk writes require preview evidence
DataSpec SHALL require operations declared with `requiresDryRun=true` to expose and verify a server-signed dry-run evidence token before confirmed apply.

#### Scenario: Strong dry-run operation declares enforced evidence
- **WHEN** an operation enforces server-side dry-run evidence at the confirmed write endpoint
- **THEN** its safety metadata sets `requiresDryRun` to `true`
- **AND** the metadata points to the preview, dry-run, compare, or plan endpoint or command in `nextActions`
- **AND** operations that only recommend preview but do not verify evidence do not set `requiresDryRun=true`.

#### Scenario: Confirmed apply references preview result
- **WHEN** a user or AI prepares to run a confirmed high-risk apply operation
- **THEN** DataSpec exposes enough dry-run summary data to review created, updated, skipped, conflict, warning, and rollback or evidence information before apply
- **AND** confirmed apply/import requests carry the preview-returned `dryRunToken` when the operation safety metadata declares `requiresDryRun=true`.

#### Scenario: Dry-run evidence is server signed
- **WHEN** DataSpec returns a `dryRunToken`
- **THEN** the token is signed by the server and does not rely only on client-submitted equality checks
- **AND** confirmed apply/import rejects forged tokens or tokens whose signed project/candidate/plan summary does not match the request.

### Requirement: Missing safety parameters return structured diagnostics
DataSpec SHALL return AI-readable safety diagnostics when a high-risk write lacks required dry-run evidence or idempotency parameters.

#### Scenario: Missing idempotency key
- **WHEN** a high-risk write declares `requiresIdempotencyKey=true`
- **AND** the caller omits the required idempotency key or equivalent parameter
- **THEN** DataSpec rejects the write with a structured diagnostic containing `code`, `category`, `retryable`, `missing`, `operation`, `safety`, and `nextActions`
- **AND** the diagnostic tells the caller how to retry with an idempotency key.

#### Scenario: Missing dry-run evidence
- **WHEN** a high-risk write declares `requiresDryRun=true`
- **AND** the caller attempts confirmed apply without the required preview, plan, compare, or dry-run confirmation evidence
- **THEN** DataSpec rejects or blocks the apply with structured diagnostic code `DRY_RUN_REQUIRED`
- **AND** the diagnostic includes `missing=["dryRunToken"]`, the affected `operation`, `safety.requiresDryRun=true`, the required preview action, and safe retry guidance.

### Requirement: Safety diagnostics do not expose secrets
DataSpec SHALL keep safety metadata and diagnostics free of raw secrets.

#### Scenario: Sensitive inputs are declared without values
- **WHEN** an operation may accept password, token, Authorization, API key, JDBC URL, DSN, connection string, or source database credential inputs
- **THEN** `sensitiveInputs` lists only parameter names or categories
- **AND** safety metadata does not include raw values.

#### Scenario: Safety error redaction
- **WHEN** DataSpec returns a safety diagnostic through API, CLI, MCP, tests, logs, or frontend-copyable output
- **THEN** the diagnostic does not contain raw token, password, Authorization header, API key, complete JDBC URL, DSN, connection string, or source database business rows.

### Requirement: Frontend dry-run summary uses safety metadata
DataSpec Web SHALL display safety-aware dry-run summaries before high-risk confirmed writes.

#### Scenario: User reviews batch write before apply
- **WHEN** a frontend flow prepares to apply a high-risk batch, import, restore, merge, or reuse operation
- **THEN** the page displays a dry-run summary using the operation safety metadata
- **AND** the summary includes counts, conflicts or warnings, idempotency or retry guidance when applicable, and next actions.

#### Scenario: Low-risk personal CRUD remains lightweight
- **WHEN** a user performs a low-risk single-record personal CRUD action that is not marked as requiring dry-run
- **THEN** DataSpec does not require the unified safety dry-run summary solely because the operation writes one record.
