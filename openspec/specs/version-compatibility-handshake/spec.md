# version-compatibility-handshake Specification

## Purpose
TBD - created by archiving change add-version-compatibility-handshake. Update Purpose after archive.
## Requirements
### Requirement: Read-only version compatibility handshake
DataSpec SHALL expose a read-only version compatibility handshake for AI agents, CLI clients, and MCP clients.

#### Scenario: Read compatibility payload
- **WHEN** a caller requests `/api/capabilities/version`
- **THEN** DataSpec returns `kind`, `schemaVersion`, `serverVersion`, `apiSchemaHash`, `minCliVersion`, `supportedCapabilities`, `deprecatedFields`, `compatibility`, `upgradeHints`, and `generatedAt`
- **AND** the request does not create projects, lint SQL, export context, connect to databases, or mutate DataSpec state.

#### Scenario: Evaluate provided client version
- **WHEN** a caller provides a CLI or MCP client version in the request
- **THEN** DataSpec compares it with `minCliVersion`
- **AND** returns `compatibility.status`, `compatibility.compatible`, `compatibility.reasons`, and `compatibility.nextActions` so AI can continue, downgrade, upgrade, or stop.

#### Scenario: Missing or unparsable client version
- **WHEN** the caller omits client version or sends a version that cannot be compared
- **THEN** DataSpec returns `compatibility.status=UNKNOWN`
- **AND** includes a next action telling the caller how to run a version-aware CLI or MCP check.

### Requirement: Compatibility handshake stays non-secret
The version compatibility handshake SHALL avoid secrets and business data.

#### Scenario: Compatibility response is safe for logs
- **WHEN** DataSpec returns the compatibility payload
- **THEN** it does not include API tokens, passwords, Authorization headers, complete JDBC URLs, DSNs, source database rows, SQL text, or project asset payloads.

#### Scenario: Capability list is descriptive only
- **WHEN** the payload lists supported capabilities
- **THEN** each entry describes stable ids, status, and optional minimum client version
- **AND** listing a capability does not execute that capability.
