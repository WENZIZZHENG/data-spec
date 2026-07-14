## ADDED Requirements

### Requirement: Token evidence candidate preview is read-only
DataSpec SHALL provide a project-scoped token evidence candidate preview before any Inbox write.

#### Scenario: Actionable evidence is ready for review
- **WHEN** an authorized caller previews a candidate with at least one actionable token signal and no field or candidate conflict
- **THEN** DataSpec returns READY with sourceType TOKEN_EVIDENCE, candidate payload, signals, willWrite false, nextActions, and a signed dryRunToken
- **AND** the candidate Inbox remains unchanged.

#### Scenario: Existing field or candidate blocks preview
- **WHEN** the candidate name already exists as a standard field, an exact token evidence fact already exists, or another active candidate uses the same name
- **THEN** preview reports STANDARD_EXISTS, EXACT_DUPLICATE, or NAME_CONFLICT with the safe existing candidate id when applicable
- **AND** it does not issue an applicable write token.

### Requirement: Token evidence candidate apply requires fresh confirmation
DataSpec SHALL create a token evidence candidate only after explicit confirmation of a matching dry run.

#### Scenario: Confirmed preview creates pending candidate
- **WHEN** an authorized caller submits the same normalized input, a valid dryRunToken, and confirmed true
- **THEN** DataSpec creates one PENDING TOKEN_EVIDENCE candidate with secret-safe structured evidence
- **AND** it does not accept, merge, ignore, postpone, modify a standard field, or modify glossary state.

#### Scenario: Token or evidence has drifted
- **WHEN** the token is invalid, belongs to another project or input, or current token evidence differs from the preview evidence hash
- **THEN** apply rejects the write with an actionable error
- **AND** the caller must run preview again.

#### Scenario: Candidate metadata has drifted
- **WHEN** candidateName, displayName, dataType, comment, sourceRef, confidence, or any other previewed candidate payload value differs at apply time
- **THEN** apply rejects the write because the signed inputHash no longer matches
- **AND** the caller must review a newly generated preview.

#### Scenario: Confirmation is absent
- **WHEN** confirmed is not true
- **THEN** apply rejects the request before inserting a candidate.

#### Scenario: Generic create attempts to claim the reserved source
- **WHEN** a caller submits sourceType TOKEN_EVIDENCE to the generic candidate create API
- **THEN** DataSpec rejects the request and directs the caller to the token evidence preview/apply API
- **AND** no candidate is inserted.

#### Scenario: Apply response hides persistence internals
- **WHEN** apply creates or deduplicates a TOKEN_EVIDENCE candidate
- **THEN** the response returns the dedicated candidate view and all stable workflow fields are required by the OpenAPI schema
- **AND** it does not expose logical deletion or other persistence-only fields.

### Requirement: Token evidence candidate writes are idempotent
DataSpec SHALL deduplicate token evidence candidate facts by projectId, candidateName, sourceType, and sourceRef at the database boundary.

#### Scenario: Apply is retried or concurrent
- **WHEN** two valid apply requests target the same token evidence fact
- **THEN** at most one row is inserted
- **AND** both callers receive the same candidate, with the non-inserting result marked deduplicated.

#### Scenario: Same name has another active source fact
- **WHEN** an active candidate has the same project and candidateName but a different sourceType or sourceRef
- **THEN** DataSpec blocks the new write as NAME_CONFLICT
- **AND** it directs the caller to review the existing candidate instead of duplicating the Inbox item.

#### Scenario: Same name different sources race across create paths
- **WHEN** token evidence apply races with generic candidate create, direct or batch standard field create, field rename or undo, candidate accept, or another apply using a different sourceRef for the same project and field name
- **THEN** DataSpec serializes the cross-table conflict check and write for that project field name
- **AND** the result cannot contain both a standard field and an active same-name candidate.

#### Scenario: Direct field creation encounters an active candidate
- **WHEN** a caller directly creates a standard field whose project and name are already reserved by a PENDING or POSTPONED candidate
- **THEN** DataSpec rejects direct creation and directs the caller to accept or merge the Inbox candidate
- **AND** candidate accept may exclude only the candidate currently being accepted from this check.

### Requirement: Token evidence candidate workflow is available in the existing workbench
DataSpec Web SHALL expose token evidence preview and confirmed apply in the project-scoped standard candidate workbench.

#### Scenario: User previews and applies evidence candidate
- **WHEN** a user enters candidate metadata and requests preview
- **THEN** the workbench displays the preview status and actionable signals without writing
- **AND** only a READY preview with explicit confirmation enables the apply action.

#### Scenario: Apply succeeds
- **WHEN** the confirmed apply succeeds
- **THEN** the workbench filters or refreshes the existing candidate list to show TOKEN_EVIDENCE candidates
- **AND** the user continues with the existing accept, merge, ignore, or postpone actions.

#### Scenario: No project is selected
- **WHEN** no current project exists
- **THEN** the token evidence entry is disabled
- **AND** no project-scoped preview or apply request is sent.

#### Scenario: Project changes while apply is in flight
- **WHEN** the current project or dialog generation changes before an apply response returns
- **THEN** the workbench ignores the stale response
- **AND** it does not refresh or filter the newly selected project's Inbox from the old result.

#### Scenario: Project or filters change while list refresh is in flight
- **WHEN** a candidate list response returns after the current project, filters, keyword, or pagination request generation has changed
- **THEN** the workbench ignores the stale list response
- **AND** candidates from the old query cannot overwrite the current Inbox view.
