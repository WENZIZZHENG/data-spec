## ADDED Requirements

### Requirement: Post-check validates structured findings
AI output post-check SHALL accept additive structured findings and validate their evidence with the current project evidence resolver.

#### Scenario: External AI submits findings
- **WHEN** a caller submits content plus bounded structured findings
- **THEN** post-check returns normalized secret-safe `findings[]`
- **AND** every evidenceRef is resolved under the request project before high-confidence or caller-declared auto-fix-safe input is accepted
- **AND** normalized external findings always return `autoFixSafe=false`
- **AND** a PASS result returns a process-local signed verificationReceipt bound to projectId, PASS/safeToUse, and the complete normalized external findings digest.

#### Scenario: Structured finding evidence is invalid
- **WHEN** a structured finding references missing, unverifiable, or cross-project evidence
- **THEN** post-check returns WARN or FAIL according to the shared evidence-gating rules
- **AND** `safeToUse` is false for a blocking failure.

#### Scenario: Post-check is not passing
- **WHEN** structured findings produce WARN or FAIL
- **THEN** the result does not issue a verificationReceipt that can authorize Evidence Package export.

#### Scenario: Structured findings are empty
- **WHEN** the caller submits no finding or an empty array and legacy content checks pass
- **THEN** post-check may return PASS with an empty findings array
- **AND** existing resolvedRefs, issues, suggestedFixes, evidenceLinks, and nextActions remain compatible.

### Requirement: Legacy post-check issues map to shared findings
AI output post-check SHALL expose existing deterministic issues as additive shared findings without removing the issue contract.

#### Scenario: Legacy reference issue is detected
- **WHEN** post-check produces a stable-reference, evidence-claim, enum, rule, or snapshot issue
- **THEN** the result keeps the existing issue
- **AND** an equivalent secret-safe finding exposes the same code, severity, subject, observed evidence, and suggested action.
