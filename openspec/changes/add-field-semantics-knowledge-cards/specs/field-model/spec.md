## ADDED Requirements

### Requirement: Field Semantic Metadata Additions
The standard field API SHALL expose optional semantic and translation metadata without breaking existing field clients.

#### Scenario: Field response includes semantic metadata
- **WHEN** a caller lists, creates, updates, retrieves, backs up, snapshots, or exports a field
- **THEN** the field contract can include optional naming translation fields and a semantic summary
- **AND** clients that ignore unknown optional fields remain compatible.

#### Scenario: Update preserves unrelated semantic fields
- **WHEN** a caller updates a field's ordinary metadata without sending naming translation fields
- **THEN** DataSpec preserves existing semantic and translation metadata unless the request explicitly clears it according to the field update contract.

### Requirement: Field Project-Safe Semantic References
Field semantic references SHALL respect project boundaries and lifecycle safety.

#### Scenario: Reject cross-project semantic reference
- **WHEN** a semantic rule or metric definition references a source, replacement, measure, or dimension field outside the current project
- **THEN** DataSpec rejects the write with a non-sensitive business validation error.
