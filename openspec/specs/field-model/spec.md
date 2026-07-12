# field-model Specification

## Purpose
Defines the standard field metadata contract used by DataSpec for personal and small-team field standards.
## Requirements
### Requirement: Standard Field Metadata
The system SHALL store personal-use metadata and optional usage contract guidance for each standard field.

#### Scenario: Create field with AI metadata
- **WHEN** a user creates a standard field with aliases, sensitivity, status, code set, category, example value, and optional usage contract values
- **THEN** the system persists those metadata fields with the field

#### Scenario: Default metadata
- **WHEN** a user creates a field without sensitivity, status, or usage contract values
- **THEN** the field defaults to `sensitive=false`
- **AND** the field defaults to `status=enabled`
- **AND** usage contract fields remain empty and compatible with existing clients

### Requirement: Field Catalog AI Export
The system SHALL include standard field metadata and usage contract guidance in AI field catalog exports.

#### Scenario: Export field aliases and governance hints
- **WHEN** AI context field catalog is generated
- **THEN** each field includes aliases as an array when aliases exist
- **AND** it includes `sensitive`, `status`, `codeSetId`, and `example` when available
- **AND** it includes an additive `usageContract` object when at least one usage contract value is present

### Requirement: Field responses include stable reference metadata
Standard field responses and AI exports SHALL expose additive stable reference metadata.

#### Scenario: Export stable field metadata
- **WHEN** a field is listed, searched, resolved, or exported to AI Context
- **THEN** it SHALL include `stableRef` and `canonicalRef`
- **AND** it SHALL include `aliasHistory` and `deprecatedRefs` when those values can be derived from current aliases, lifecycle replacement, change logs, or snapshots.

#### Scenario: Existing field clients remain compatible
- **WHEN** stable reference metadata is added
- **THEN** existing field IDs, names, aliases, status, and usage contract fields SHALL keep their documented semantics.

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
