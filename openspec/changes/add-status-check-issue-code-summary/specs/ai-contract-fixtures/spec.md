## ADDED Requirements

### Requirement: Status check summarizes issue codes
The local status check JSON output SHALL include issue code summaries so AI agents can distinguish repeated instances of the same problem type from distinct problem types.

#### Scenario: Repeated warning code is summarized
- **WHEN** the status check emits multiple warning issues with the same code
- **THEN** `summary.issueCodes[]` contains one item for that code.
- **AND** that item includes the total count and `severity` equal to `warning`.

#### Scenario: Error code is summarized
- **WHEN** the status check emits error issues
- **THEN** `summary.issueCodes[]` contains the error code with its count.
- **AND** that item reports `severity` equal to `error`.

#### Scenario: Existing issue details remain authoritative
- **WHEN** issue code summaries are emitted
- **THEN** existing `issues[]`, `checks[]`, `summary.errors`, `summary.warnings`, `status`, and exit code semantics remain unchanged.
