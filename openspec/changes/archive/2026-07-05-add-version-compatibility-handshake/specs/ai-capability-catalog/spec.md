## ADDED Requirements

### Requirement: Capability catalog includes version compatibility handshake
The AI capability catalog SHALL describe the version compatibility handshake as a read-only preflight capability.

#### Scenario: List version compatibility capability
- **WHEN** a caller requests the capability catalog
- **THEN** the catalog includes a stable `version-compatibility` capability
- **AND** the capability lists `/api/capabilities/version`, CLI `compat check`, and MCP `dataspec://version-compatibility` surfaces.

#### Scenario: Recommended first actions mention compatibility
- **WHEN** the catalog returns `recommendedFirstActions`
- **THEN** at least one recommended action tells AI clients to check version compatibility before executing CLI or MCP workflows that depend on server capabilities.

#### Scenario: Version compatibility safety metadata
- **WHEN** the catalog describes `version-compatibility`
- **THEN** it marks the capability as read-only
- **AND** its safety metadata states that it does not write project state, connect to source databases, or expose secrets.
