## ADDED Requirements

### Requirement: Evidence package can reference task runs
AI evidence packages SHALL be able to include AI task run state for handoff and recovery.

#### Scenario: Evidence from task run
- **WHEN** a caller exports evidence for an AI task run
- **THEN** the package includes task status, failed step, retryable state, resume command, source reference, partial artifacts, validation summary, and next actions.

#### Scenario: Evidence task run redaction
- **WHEN** task run metadata or artifacts are included in an evidence package
- **THEN** the package MUST NOT include token, password, Authorization header, full JDBC URL, or sampled business data rows.
