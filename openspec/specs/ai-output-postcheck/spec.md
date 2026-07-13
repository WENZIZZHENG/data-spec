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
