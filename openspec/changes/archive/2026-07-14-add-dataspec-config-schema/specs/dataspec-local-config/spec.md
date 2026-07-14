## ADDED Requirements

### Requirement: Local config exposes a versioned JSON Schema
DataSpec SHALL provide a versioned JSON Schema for the supported `.dataspec/config.json` fields and SHALL preserve legacy runtime compatibility.

#### Scenario: Editor reads local schema association
- **WHEN** `.dataspec/config.json` declares `$schema` as `./config.schema.json` and `configVersion` as `1`
- **THEN** the local schema describes every supported top-level and securityProfile field with type, boundary, and sensitive-value guidance
- **AND** a generic JSON Schema aware editor can validate the config without a DataSpec-specific plugin or network request.

#### Scenario: Legacy config omits schema metadata
- **WHEN** an existing config omits `$schema` or `configVersion`
- **THEN** CLI and MCP continue reading the existing supported fields with the same precedence and defaults
- **AND** no DataSpec HTTP request is blocked solely because schema metadata is absent.

#### Scenario: Config contains unknown extension fields
- **WHEN** a config contains fields not consumed by the current loader
- **THEN** runtime loading ignores those fields for backward compatibility
- **AND** editor-safe private extensions can use an `x-` prefixed property defined by the schema.

#### Scenario: Schema metadata has invalid types
- **WHEN** `$schema` is not a string or `configVersion` is not a positive integer
- **THEN** local config loading returns a readable error before any DataSpec HTTP API call.

#### Scenario: Legacy project id string is normalized safely
- **WHEN** projectId is a positive integer or a legacy string containing only positive decimal digits
- **THEN** the loader returns the same positive integer project id
- **AND** boolean, object, negative, zero, or mixed-text values are rejected instead of being coerced.
