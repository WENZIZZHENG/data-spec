## MODIFIED Requirements

### Requirement: AI Context package includes capability catalog
The AI Context package SHALL include the DataSpec capability catalog so offline agents can discover supported workflows and surfaces.

#### Scenario: Export package with capabilities
- **WHEN** an AI Context zip is generated
- **THEN** it contains `.dataspec/capabilities.json`
- **AND** manifest files list the capability catalog artifact.

#### Scenario: Exported capabilities include standard evidence
- **WHEN** an AI Context zip is generated
- **THEN** `.dataspec/capabilities.json` includes `standard-evidence`
- **AND** `standard-evidence` is read-only, API-only, and lists `GET /api/standard-evidence`.

#### Scenario: Capability catalog is documented in package README
- **WHEN** a caller reads the package README or AGENTS fragment
- **THEN** it instructs AI agents to read `.dataspec/capabilities.json` before selecting CLI, MCP, or API actions.

#### Scenario: Offline cache includes capabilities
- **WHEN** CLI export-context writes an offline `.dataspec/context/` cache
- **THEN** the cache includes the capability catalog file from the exported package
- **AND** the file does not contain secrets or business data rows.
