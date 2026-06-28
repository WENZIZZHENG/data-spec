## ADDED Requirements

### Requirement: Project AI feedback report
DataSpec SHALL provide a project-scoped, read-only AI feedback report based on existing DataSpec records.

#### Scenario: Report aggregates existing AI usage signals
- **WHEN** a caller requests the AI feedback report for a project
- **THEN** DataSpec returns summary metrics, field signals, rule signals, fixedSql signals, unmanaged or standardization signals, next actions, sample size, and generatedAt
- **AND** the report is derived from existing AI job records, SQL check records, rule exemptions, reverse import field sources, and standard field metadata.

#### Scenario: Report remains project scoped
- **WHEN** the feedback report is requested with a projectId
- **THEN** DataSpec MUST enforce project access checks
- **AND** the report MUST NOT include records from other projects.

### Requirement: Feedback report does not collect sensitive behavior data
DataSpec SHALL avoid turning the feedback report into user behavior monitoring or external analytics.

#### Scenario: No business rows or secrets
- **WHEN** the feedback report is generated
- **THEN** it MUST NOT read source database row data
- **AND** it MUST NOT expose API tokens, database passwords, bearer tokens, or full JDBC URLs.

#### Scenario: No automatic standard writes
- **WHEN** the report includes suggested actions
- **THEN** DataSpec MUST present them as navigation or manual next steps
- **AND** it MUST NOT automatically create aliases, fields, rule exemptions, or standard changes.

### Requirement: Feedback report explains confidence and gaps
DataSpec SHALL distinguish evidence-backed feedback from missing instrumentation.

#### Scenario: Recommendation history is insufficient
- **WHEN** DataSpec cannot derive accurate recommendation hit or miss rates from existing records
- **THEN** the report returns an explicit gap flag or note
- **AND** it suggests adding future recommendation event tracking rather than inventing a rate.

#### Scenario: Signals include evidence
- **WHEN** a field, rule, fixedSql, or unmanaged signal is returned
- **THEN** the signal includes a title, count, severity, evidence source, suggested action, and optional target route.

### Requirement: Frontend AI feedback page
DataSpec Web SHALL provide a project-scoped AI feedback page for the report.

#### Scenario: View feedback report
- **WHEN** a user opens the AI feedback page with a current project
- **THEN** the page displays summary metrics, high-priority signals, rule issue ranking, fixedSql opportunities, standardization signals, and next actions.

#### Scenario: Navigate to standard maintenance
- **WHEN** a feedback signal has a target route
- **THEN** the user can open the related field library, field quality, rule configuration, rule exemption, SQL lint record, or AI replay page.

#### Scenario: No project selected
- **WHEN** no project is selected
- **THEN** the page shows an empty state
- **AND** it does not call project-scoped feedback APIs.
