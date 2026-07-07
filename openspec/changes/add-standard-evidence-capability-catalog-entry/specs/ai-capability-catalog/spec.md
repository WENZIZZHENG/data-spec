## ADDED Requirements

### Requirement: Capability catalog includes standard evidence view
The AI capability catalog SHALL describe the standard evidence view as a read-only capability for inspecting a single standard field's cross-source evidence.

#### Scenario: List standard evidence capability
- **WHEN** a caller lists the AI capability catalog
- **THEN** the catalog includes a stable `standard-evidence` capability
- **AND** the capability lists the `GET /api/standard-evidence` API surface
- **AND** the capability provides an example request `GET /api/standard-evidence?projectId=<id>&subjectType=FIELD&subjectId=<fieldId>`.

#### Scenario: Standard evidence capability safety
- **WHEN** the catalog describes `standard-evidence`
- **THEN** it marks the capability as read-only
- **AND** its safety metadata states that it does not write project state, export raw SQL, expose AI payloads, expose candidate raw evidence, or expose credentials.

#### Scenario: Retrieve standard evidence capability
- **WHEN** a caller requests the `standard-evidence` capability by id
- **THEN** DataSpec returns the capability entry with input summary, output contract, example request, preflight checks and next actions.
