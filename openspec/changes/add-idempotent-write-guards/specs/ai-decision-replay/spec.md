## ADDED Requirements

### Requirement: AI job replay record deduplication
AI job replay records SHALL avoid duplicate rows for the same stable operation input during automatic retries.

#### Scenario: Same AI job fingerprint is recorded twice
- **WHEN** a service attempts to record an AI job with the same project, job type, prompt version, input payload, output payload, snapshot reference, and linked SQL check record
- **THEN** DataSpec reuses the first record instead of inserting a duplicate row.
