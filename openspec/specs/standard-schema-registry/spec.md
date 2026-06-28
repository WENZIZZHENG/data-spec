# standard-schema-registry Specification

## Purpose
TBD - created by archiving change add-schema-registry-contract-versions. Update Purpose after archive.
## Requirements
### Requirement: Schema registry catalog
DataSpec SHALL expose a machine-readable schema registry catalog for AI-consumed standard contracts.

#### Scenario: List registry catalog
- **WHEN** a client requests the schema registry catalog
- **THEN** the response includes `kind`, `schemaVersion`, `registryVersion`, `compatibilityPolicy`, and `contracts[]`
- **AND** every contract summary includes `contractId`, `displayName`, `schemaVersion`, `stableFields`, `deprecatedFields`, `jsonSchemaRef`, and `docsRef`.

#### Scenario: Registry contains core contracts
- **WHEN** the schema registry catalog is generated
- **THEN** it contains contracts for Field, Enum, Rule, Template, StandardSnapshot, LintResult, AI Context manifest, AI Context field catalog, and AI task profile.

### Requirement: Contract detail
DataSpec SHALL expose a detail view for each registered standard contract.

#### Scenario: Show contract detail
- **WHEN** a client requests a known contract id
- **THEN** the response includes the contract metadata, JSON Schema, stable field paths, deprecated field metadata, compatibility window, and examples when available.

#### Scenario: Unknown contract
- **WHEN** a client requests an unknown contract id
- **THEN** DataSpec returns a readable failure that includes supported contract ids.

### Requirement: Compatibility policy
The registry SHALL define a compatibility policy for AI-consumed contract changes.

#### Scenario: Compatible additive field
- **WHEN** a contract gains an optional field without changing documented stable semantics
- **THEN** the registry marks this as a compatible change.

#### Scenario: Breaking contract change
- **WHEN** a stable field is removed, renamed, changes type, or changes documented semantics
- **THEN** the registry policy requires a schemaVersion update, fixture update, and migration note.

#### Scenario: Deprecated field
- **WHEN** a field is deprecated but still present
- **THEN** the contract lists the field path, replacement hint, deprecatedSince, removalAfter, and reason.

