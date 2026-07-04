## ADDED Requirements

### Requirement: AI batch records task run state
AI batch SQL lint SHALL create or update an AI task run record for retry and resume diagnostics.

#### Scenario: Batch run succeeds with task run
- **WHEN** a caller creates an AI batch SQL lint run
- **THEN** DataSpec links the batch run to a task run with status `SUCCEEDED`
- **AND** the delivery package includes the task run id and resume metadata.

#### Scenario: Batch run partially fails
- **WHEN** one or more batch lint items fail while other items complete
- **THEN** DataSpec records task run status `PARTIAL_FAILED`, stores completed item artifacts, marks retryable when the failed item can be retried, and returns the original batch delivery package shape plus task run metadata.

#### Scenario: Duplicate retry keeps one task run
- **WHEN** the same batch request is retried with the same idempotency key
- **THEN** DataSpec reuses the original delivery package and task run instead of creating duplicate task run records.
