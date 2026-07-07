# ai-task-runs Specification

## Purpose
定义项目级 AI 任务运行记录和重试恢复诊断，用于追踪任务生命周期、失败步骤、部分产物、幂等键、恢复命令和安全的元数据摘要。
## Requirements
### Requirement: Persist AI task runs
DataSpec SHALL persist project-scoped AI task run records for retry and resume diagnostics.

#### Scenario: Record task run lifecycle
- **WHEN** an AI-facing task starts, succeeds, partially fails, or fails
- **THEN** DataSpec records task type, status, source reference, input hash, idempotency key when available, step statuses, retryable flag, failed step, resume command, partial artifacts, metadata, started time, finished time, and expiry time.

#### Scenario: Task run metadata is secret safe
- **WHEN** a task run is persisted
- **THEN** metadata and partial artifact summaries MUST NOT include API token, database password, bearer token, full JDBC URL, or sampled business data rows.

### Requirement: Query resumable AI tasks
DataSpec SHALL expose project-scoped task run queries for users, CLI, and AI agents.

#### Scenario: List task runs
- **WHEN** a caller lists task runs for a project with optional status or taskType filters
- **THEN** DataSpec returns paginated runs ordered by newest first with stable summary fields.

#### Scenario: List recent failures
- **WHEN** a caller asks for recent failed or partially failed task runs for a project
- **THEN** DataSpec returns retryable diagnostics, failed step, resume command, next action, and partial artifact summary without requiring large payloads.

#### Scenario: View task run detail
- **WHEN** a caller requests one task run detail by id and project id
- **THEN** DataSpec returns parsed step statuses, partial artifacts, metadata, and source reference
- **AND** rejects cross-project access.

### Requirement: Retry guidance contract
DataSpec SHALL provide deterministic retry guidance instead of automatic background execution.

#### Scenario: Retryable task failure
- **WHEN** a task fails at a step that can be safely retried
- **THEN** the task run includes `retryable=true`, `failedStep`, `resumeCommand`, and `nextAction`.

#### Scenario: Non-retryable task failure
- **WHEN** a task fails because of validation, missing project access, or unsafe credentials
- **THEN** the task run includes `retryable=false` and a readable next action that tells AI not to retry blindly.

