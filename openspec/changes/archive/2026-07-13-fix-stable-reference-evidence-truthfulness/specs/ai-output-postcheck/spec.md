## ADDED Requirements

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
