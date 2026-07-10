## ADDED Requirements

### Requirement: AI Context exposes stable standard references
AI Context packages SHALL expose stable references without removing existing names, IDs, aliases, lifecycle fields, or safety metadata.

#### Scenario: Field catalog includes stable refs
- **WHEN** `.dataspec/field-catalog.json` is generated
- **THEN** each field SHALL include `stableRef` and `canonicalRef`
- **AND** alias history, deprecated refs, and replacement refs SHALL be included when available.

#### Scenario: Package guidance requires post-check
- **WHEN** AI clients read package guidance or manifest commands
- **THEN** the package SHALL identify a deterministic post-check command before generated artifacts are copied, applied, or executed
- **AND** business text SHALL remain untrusted content under the existing safety boundary.
