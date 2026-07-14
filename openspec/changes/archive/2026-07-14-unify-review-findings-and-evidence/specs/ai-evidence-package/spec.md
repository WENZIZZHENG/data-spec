## ADDED Requirements

### Requirement: Evidence packages carry verified findings
AI evidence packages SHALL expose additive shared findings while preserving existing source, summary, artifact, diagnostic, and post-check fields.

#### Scenario: Package is generated from a SQL check
- **WHEN** an evidence package reads a persisted SQL check record
- **THEN** it first requires access to the record's project and derives findings from the stored lint issues
- **AND** each finding includes the canonical SQL check evidence ref and suppression waiver semantics where applicable.

#### Scenario: SQL check belongs to another project
- **WHEN** a project-scoped caller requests a SQL check record outside its authorized projects
- **THEN** package generation fails with the project access error before issue or replay content is exposed.

#### Scenario: SQL check has no persisted project owner
- **WHEN** a SQL check record has no projectId, even if the request supplies one
- **THEN** package generation fails closed before issue or replay content is exposed
- **AND** the request projectId is not used as a fallback owner.

#### Scenario: Coverage payload targets an unauthorized project
- **WHEN** a project-scoped caller requests JSON or zip evidence for a COVERAGE_REPORT project it cannot access
- **THEN** package generation fails before reading or sanitizing the report payload or submitted findings.

#### Scenario: Package receives external findings
- **WHEN** a package request includes external findings
- **THEN** it requires a successful post-check summary and matching process-local postCheckReceipt
- **AND** the receipt binds projectId, PASS/safeToUse, and the complete normalized finding digest
- **AND** the receipt is reusable for the same project and normalized findings within one service process and does not bind sourceType or sourceId
- **AND** the package revalidates evidence refs in the package project
- **AND** it rejects unverified findings instead of packaging them as trusted claims
- **AND** packaged external findings always set `autoFixSafe=false`
- **AND** the receipt itself is not persisted in the evidence package.

#### Scenario: Post-check summary or finding payload is forged
- **WHEN** a caller omits or tampers with the receipt, changes severity, evidenceRefs, confidence, or another normalized finding field after post-check, or reuses a receipt for another project
- **THEN** Evidence Package rejects the external findings before packaging them.

#### Scenario: Package has no findings
- **WHEN** the source contains no issue and the request submits no finding
- **THEN** the package returns an empty findings array without changing existing package fields.

### Requirement: Finding packages exclude unsafe payloads
Evidence Package finding output SHALL contain only bounded structured fields and canonical evidence refs.

#### Scenario: Finding contains raw output or secrets
- **WHEN** a finding or post-check summary contains raw AI output, token, password, Authorization, JDBC URL, DSN, or business row text
- **THEN** the package redacts or rejects the unsafe value
- **AND** it never stores the GitHub token or complete external response body.
