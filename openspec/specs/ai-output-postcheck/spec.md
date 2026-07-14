# ai-output-postcheck Specification

## Purpose
定义对 AI 生成 SQL、DDL、Markdown、JSON 和纯文本执行确定性只读后置检查的契约。
## Requirements
### Requirement: Deterministic AI output post-check
DataSpec SHALL provide a project-scoped, read-only post-generation check for AI-produced SQL, DDL, Markdown, JSON, and plain text.

#### Scenario: Check supported AI output
- **WHEN** a caller submits AI output text with projectId, content type, and optional standard snapshot reference
- **THEN** DataSpec SHALL return `kind`, `schemaVersion`, `status`, `safeToUse`, `summary`, `issues`, `resolvedRefs`, `suggestedFixes`, `evidenceLinks`, and `nextActions`
- **AND** the check SHALL NOT modify standards, AI jobs, business files, or databases.

#### Scenario: Detect unknown standard reference
- **WHEN** a high-confidence SQL, DDL, explicit stableRef, enum ref, rule ref, or snapshot ref does not resolve in the selected project
- **THEN** the check SHALL report an issue with the unknown reference, source location or bounded excerpt, severity, and suggested next action
- **AND** the result SHALL be `FAIL` when the unknown reference can change the correctness of the generated artifact.

#### Scenario: Detect stale or replaced reference
- **WHEN** output references a deprecated, disabled, replaced, merged, or old-snapshot standard object
- **THEN** the check SHALL report a stale-reference issue
- **AND** it SHALL include the current canonicalRef or replacementRef when available.

#### Scenario: Validate enums rules and evidence claims
- **WHEN** output explicitly claims an enum value, rule code, snapshot version, or DataSpec evidence ref
- **THEN** DataSpec SHALL validate that claim against current project standards or the requested snapshot
- **AND** invalid enum values, unknown rules, snapshot drift, and missing evidence refs SHALL be reported deterministically.

### Requirement: Post-check status semantics
Post-check results SHALL use stable PASS, WARN, and FAIL semantics.

#### Scenario: Passing result
- **WHEN** all high-confidence references resolve to current compatible standards and no blocking issue exists
- **THEN** `status` SHALL be `PASS`
- **AND** `safeToUse` SHALL be true.

#### Scenario: Warning result
- **WHEN** only ambiguous, low-confidence, stale-but-compatible, or evidence-gap issues exist
- **THEN** `status` SHALL be `WARN`
- **AND** `safeToUse` SHALL be false unless every warning is explicitly classified as non-blocking.

#### Scenario: Failing result
- **WHEN** an unknown high-confidence field, invalid enum value, unknown rule, incompatible snapshot, or unsafe unsupported claim exists
- **THEN** `status` SHALL be `FAIL`
- **AND** `safeToUse` SHALL be false.

### Requirement: Post-check output is bounded and secret-safe
DataSpec SHALL bound post-check input and output and redact secret-like content.

#### Scenario: Unsafe or oversized input
- **WHEN** AI output is empty, exceeds the documented size limit, or contains unsupported binary content
- **THEN** DataSpec SHALL return a readable validation error before parsing.

#### Scenario: Secret-like AI output
- **WHEN** AI output, excerpts, diagnostics, fixes, or evidence labels contain token, password, Authorization, JDBC URL, DSN, or connection-string-like text
- **THEN** API, CLI, MCP, evidence package, logs, and errors SHALL NOT expose the raw value.

### Requirement: Project-scoped evidence claims are resolved deterministically
DataSpec SHALL resolve supported `dataspec://evidence/<source-type>/<source-id>` claims against persisted sources in the selected project before reporting an AI output as safe to use.

#### Scenario: Evidence claim is verified
- **WHEN** an AI output contains a supported evidence ref whose persisted source exists in the selected project
- **THEN** post-check does not emit an evidence issue for that claim
- **AND** the canonical evidence ref appears in `evidenceLinks`.

#### Scenario: Evidence source is missing
- **WHEN** an AI output contains a syntactically valid supported evidence ref whose source record does not exist
- **THEN** post-check emits `MISSING_EVIDENCE_REFERENCE` with WARN severity
- **AND** the result is not reported as safe to use without review.

#### Scenario: Evidence source belongs to another project
- **WHEN** an AI output contains a supported evidence ref whose source record belongs to another project
- **THEN** post-check emits `CROSS_PROJECT_EVIDENCE_REFERENCE` with FAIL severity
- **AND** it does not expose the source title, status, payload, or owning project metadata.

#### Scenario: Evidence claim format cannot be verified
- **WHEN** an AI output contains an unsupported, malformed, payload-only, or otherwise non-resolvable evidence ref
- **THEN** post-check emits `UNVERIFIABLE_EVIDENCE_REFERENCE` with WARN severity
- **AND** it does not treat the claimed URI as verified evidence.

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
