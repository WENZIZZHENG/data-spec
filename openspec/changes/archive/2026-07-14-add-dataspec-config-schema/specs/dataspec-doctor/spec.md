## ADDED Requirements

### Requirement: Doctor reports local config schema compatibility
DataSpec doctor SHALL expose a secret-safe summary of the local config schema and version state without introducing a second runtime fingerprint protocol.

#### Scenario: Config uses the supported schema
- **WHEN** config declares the supported version and local schema reference and the schema file exists
- **THEN** doctor reports supportedVersion, declaredVersion, effectiveVersion, schemaRef, schemaPath, schemaFilePresent, and associationStatus
- **AND** the config check passes without exposing apiToken, security patterns, or local-only path values
- **AND** a non-canonical schemaRef is represented by a fixed safe placeholder rather than its raw value.

#### Scenario: Legacy config has no association
- **WHEN** config omits `$schema`, `configVersion`, or the local schema file
- **THEN** doctor keeps the config usable and returns a warning with the effective legacy version and a migration hint.

#### Scenario: Config declares an unsupported future version
- **WHEN** configVersion is greater than the current doctor supportedVersion
- **THEN** the config check fails and tells the caller to upgrade the CLI or use a supported config
- **AND** doctor does not silently claim the future version is compatible.

#### Scenario: No local config exists
- **WHEN** doctor cannot find `.dataspec/config.json`
- **THEN** it reports the supported schema version and expected local schema filename alongside the existing missing-config warning.
