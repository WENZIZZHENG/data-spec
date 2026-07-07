## ADDED Requirements

### Requirement: Status check text output summarizes check severity counts
The local status check text output SHALL summarize each check's status and severity counts so readers can locate warning-only and error checks without parsing the full issue list.

#### Scenario: Text output includes check summary section
- **WHEN** the status check formats a report as text
- **THEN** the output includes a `检查项:` section before issue details.

#### Scenario: Check summary exposes severity counts
- **WHEN** a check has warning or error issues
- **THEN** its text summary line includes the check id, check status, total issue count, error count, and warning count.

#### Scenario: JSON contract remains unchanged
- **WHEN** status-check output is requested as JSON
- **THEN** the existing JSON fields and status semantics remain unchanged.
