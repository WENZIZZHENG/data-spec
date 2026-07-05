## ADDED Requirements

### Requirement: Capability entries expose write safety metadata
The AI capability catalog SHALL expose the unified AI write safety metadata for every capability entry.

#### Scenario: List capability safety
- **WHEN** a caller requests the capability catalog
- **THEN** each capability includes a `safety` object with `readOnly`, `writesProject`, `requiresDryRun`, `supportsUndo`, `requiresIdempotencyKey`, `sensitiveInputs`, and `nextActions`
- **AND** existing `writeRisk`, `preflightChecks`, and top-level `nextActions` fields remain available for compatible clients.

#### Scenario: Show capability safety
- **WHEN** a caller requests a single capability by id
- **THEN** the response includes the same `safety` object used in the list catalog
- **AND** the safety object does not execute the capability or grant extra permission.

#### Scenario: High-risk capability guidance
- **WHEN** a capability can perform high-risk project writes such as standard merge, reverse import confirmation, project restore apply, standard reuse apply, starter kit apply, or AI batch writes
- **THEN** the catalog marks `safety.requiresDryRun` or `safety.requiresIdempotencyKey` according to that operation's required safeguards
- **AND** `safety.nextActions` points to the dry-run, preview, idempotency, evidence, or recovery step.

#### Scenario: Catalog safety remains non-secret
- **WHEN** the catalog describes operations with sensitive inputs
- **THEN** `safety.sensitiveInputs` lists only safe parameter names or categories
- **AND** the catalog response does not include API tokens, passwords, Authorization headers, complete JDBC URLs, DSNs, or source database rows.
