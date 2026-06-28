# ai-batch-delivery-package Specification

## Purpose
DataSpec provides a machine-readable delivery package for AI batch SQL lint work so agents and users can review, download, replay, and continue from a stable result contract.
## Requirements
### Requirement: AI batch delivery package
DataSpec SHALL provide a machine-readable delivery package for batch SQL lint work.

#### Scenario: Batch package contains stable summary and item results
- **WHEN** a caller runs a batch SQL lint task for a project
- **THEN** DataSpec returns a package containing packageVersion, batchId or local batch key, projectId, batchType, source, status, summary, items, issueSummary, fixedSqlSummary, evidence, nextActions, and createdAt
- **AND** each item includes itemName or filePath, lint counts, issues, fixedSql metadata, dialect diagnostics, and any related SQL check record id when available.

#### Scenario: Batch package is secret safe
- **WHEN** a batch package is created or downloaded
- **THEN** it MUST NOT include API token, database password, bearer token, full JDBC URL, or source database row data
- **AND** paths and SQL text are included only as user-submitted task evidence.

### Requirement: Backend stores and exposes recent batch runs
DataSpec SHALL persist lightweight batch run summaries and package payloads for frontend and AI replay use.

#### Scenario: Create SQL lint batch run
- **WHEN** a caller submits projectId, source, and SQL lint items to the batch API
- **THEN** the system runs lint for each item, stores the batch summary and package payload, and returns the delivery package
- **AND** failure of one item is represented in that item result without discarding successful item results.

#### Scenario: List batch runs
- **WHEN** a caller requests batch runs for a project
- **THEN** the system returns a paginated list ordered by newest first with batch id, type, source, status, summary counts, createdAt, and operator.

#### Scenario: Download batch package
- **WHEN** a caller downloads a stored batch run
- **THEN** the system returns the original JSON delivery package for that run.

### Requirement: CLI can emit a batch delivery package
The DataSpec CLI SHALL allow `lint-files` users to write a batch delivery package without breaking existing output.

#### Scenario: Existing lint-files JSON remains compatible
- **WHEN** a user runs `lint-files --format json` without a delivery package option
- **THEN** the CLI keeps the existing JSON output shape and ERROR-based exit code behavior.

#### Scenario: lint-files writes package file
- **WHEN** a user runs `lint-files` with a delivery package output path
- **THEN** the CLI writes a JSON delivery package containing the same stable fields as the backend package
- **AND** stdout still reports the requested text or JSON output.

### Requirement: Frontend shows batch runs and package details
DataSpec Web SHALL provide a project-scoped batch run view for AI batch delivery packages.

#### Scenario: View recent batch runs
- **WHEN** a user opens the batch run page with a current project
- **THEN** the page lists recent batch runs with status, source, total items, issue counts, fixedSql count, createdAt, and download action.

#### Scenario: View batch detail
- **WHEN** a user opens a batch run detail
- **THEN** the page shows item-level results, issues, fixedSql availability, evidence, and next actions
- **AND** the user can download the JSON package.

#### Scenario: No project selected
- **WHEN** no project is selected
- **THEN** the page shows an empty state and does not call project-scoped batch APIs.

### Requirement: AI batch write idempotency
AI batch SQL lint creation SHALL support idempotency keys and project-operation locking.

#### Scenario: Retry AI batch with same key
- **WHEN** a caller retries the same batch SQL lint request with the same idempotency key
- **THEN** DataSpec returns the original delivery package without creating another batch run record.

#### Scenario: Concurrent AI batch for same project
- **WHEN** a batch SQL lint request is already running for the project
- **THEN** another batch request for the same project and operation receives a retryable conflict diagnostic unless it can reuse a completed idempotency result.
