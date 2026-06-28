## ADDED Requirements

### Requirement: AI Context package carries schema registry
The AI Context package SHALL include schema registry metadata so offline AI clients can verify output contract versions.

#### Scenario: Package includes schema registry file
- **WHEN** the AI Context package is generated
- **THEN** it contains `.dataspec/schema-registry.json`
- **AND** that file includes the registry catalog, core contract ids, and compatibility policy.

#### Scenario: Manifest references registry
- **WHEN** the AI Context manifest is generated
- **THEN** it includes a `contracts` summary with registry schemaVersion, registryVersion, registry file path, and contract ids used by the package.

#### Scenario: Existing package layout remains compatible
- **WHEN** an existing client reads `.dataspec/manifest.json`, `.dataspec/field-catalog.json`, or `.dataspec/rules.yaml`
- **THEN** existing required fields remain present and the registry metadata is additive.
