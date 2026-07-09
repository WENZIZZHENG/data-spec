## ADDED Requirements

### Requirement: Scoped export reports safety impact
Scoped AI Context export SHALL report safety impact alongside existing scope metadata.

#### Scenario: Scoped export includes safety counts
- **WHEN** a caller exports AI Context with scope, query, status, profile, task type, or limit parameters
- **THEN** `.dataspec/manifest.json` SHALL include safety counts for returned fields, restricted fields, redacted values, and warnings
- **AND** `.dataspec/field-catalog.json` SHALL keep existing `contextScope` metadata when scope metadata is applicable.

#### Scenario: Sensitive field exclusion is explainable
- **WHEN** a sensitive or redacted field appears in a scoped field catalog
- **THEN** the field SHALL include an export decision reason
- **AND** the package safety summary SHALL allow AI clients to explain why exposure was limited.
