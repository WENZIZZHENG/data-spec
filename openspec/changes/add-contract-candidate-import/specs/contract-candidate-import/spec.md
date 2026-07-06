## ADDED Requirements

### Requirement: Contract Candidate Preview Package
DataSpec SHALL generate a deterministic, project-scoped contract candidate preview package from supported schema contract sources.

#### Scenario: Preview supported contract source
- **WHEN** a caller submits `projectId`, `sourceKind` equal to `openapi`, `json-schema`, or `protobuf`, a source path, and contract content
- **THEN** the response includes stable fields `kind`, `schemaVersion`, `projectId`, `sourceKind`, `sourcePath`, `contractHash`, `summary`, `candidateFields`, `diagnostics`, `safety`, and `nextActions`.
- **AND** each `candidateFields[]` item includes deterministic `candidateKey`, `candidateName`, `displayName`, `dataType`, `required`, `enumValues`, `exampleValues`, `sourcePath`, `schemaVersion`, `confidence`, `conflictReasons`, `recommendedAction`, and `inboxPayload`.

#### Scenario: Contract hash is deterministic
- **WHEN** the same project id, source kind, source path, contract content, schema version, and parsing parameters are submitted twice
- **THEN** the generated preview uses the same `contractHash`.
- **AND** when any of those inputs changes
- **THEN** the `contractHash` changes.

#### Scenario: Unsupported source kind is rejected
- **WHEN** a caller submits an unsupported `sourceKind`
- **THEN** DataSpec rejects the request with a non-sensitive validation diagnostic that lists supported source kinds.

### Requirement: Contract Field Extraction
DataSpec SHALL extract field candidates from OpenAPI, JSON Schema, and Protobuf contract inputs without requiring external network access.

#### Scenario: Extract OpenAPI schema properties
- **WHEN** an OpenAPI contract contains component schemas, request bodies, or response schemas with object properties
- **THEN** DataSpec extracts property names, types, descriptions, required flags, enum values, example values, and source paths into candidate fields.

#### Scenario: Extract JSON Schema properties
- **WHEN** a JSON Schema contract contains object properties and required arrays
- **THEN** DataSpec extracts candidate fields with JSON pointer-like source paths and required flags.

#### Scenario: Extract Protobuf fields
- **WHEN** a Protobuf `.proto` text or descriptor-style JSON contains message fields
- **THEN** DataSpec extracts field names, scalar or message types, comments when available, field numbers when available, and source paths.

#### Scenario: Complex schema composition is conservative
- **WHEN** a contract uses unsupported `oneOf`, `anyOf`, `allOf`, `$ref`, generics, or nested constructs that cannot be resolved deterministically
- **THEN** DataSpec emits diagnostics and marks affected candidates with `recommendedAction=REVIEW_REQUIRED` instead of pretending the extraction is complete.

### Requirement: Contract Candidate Deduplication And Safety
Contract candidate import SHALL be read-only, deduplicated, and safe for AI-assisted use.

#### Scenario: Existing standards are matched
- **WHEN** a candidate name matches an existing project standard field
- **THEN** the preview marks the candidate with `recommendedAction=MERGE_EXISTING`
- **AND** includes conflict or match reasons without modifying the existing field.

#### Scenario: Duplicate contract fields are controlled
- **WHEN** the same normalized field appears multiple times in one contract preview
- **THEN** DataSpec returns one stable candidate entry with source evidence summarized
- **AND** records duplicate source paths in conflict or evidence metadata.

#### Scenario: Preview has no write side effects
- **WHEN** a caller previews a contract import through API or CLI
- **THEN** DataSpec does not create, update, delete, import, export, or persist project records.
- **AND** the `safety` object declares `readOnly=true`, `writesProject=false`, `externalNetworkUsed=false`, `externalLlmUsed=false`, and `containsRealBusinessRows=false`.

#### Scenario: Sensitive content is redacted
- **WHEN** contract content, source paths, descriptions, examples, diagnostics, or errors include token, password, Authorization header, API key, complete JDBC URL, DSN, or connection string patterns
- **THEN** the preview package and CLI output redact those values.
