# ai-context-package Specification

## Purpose
TBD - created by archiving change add-ai-context-package. Update Purpose after archive.
## Requirements
### Requirement: Downloadable AI Context Package
The system SHALL provide a downloadable AI Context zip package for a project.

#### Scenario: Download package
- **WHEN** a caller requests the AI Context package for a valid `projectId`
- **THEN** the system returns a zip file response named `dataspec-ai-context.zip`
- **AND** the response media type is `application/zip`

### Requirement: Stable Package Layout
The AI Context package SHALL contain a stable `.dataspec/` layout and an agent instruction fragment.

#### Scenario: Package contains required files
- **WHEN** the AI Context package is generated
- **THEN** it contains `.dataspec/DATABASE_RULES.md`
- **AND** it contains `.dataspec/field-catalog.json`
- **AND** it contains `.dataspec/field-catalog.schema.json`
- **AND** it contains `.dataspec/rules.yaml`
- **AND** it contains `.dataspec/prompts.md`
- **AND** it contains `.dataspec/examples/good.sql`
- **AND** it contains `.dataspec/examples/bad.sql`
- **AND** it contains `AGENTS.md.fragment`

### Requirement: Machine Readable Field Catalog Contract
The AI Context package SHALL include a JSON Schema describing the generated field catalog.

#### Scenario: Schema matches generated catalog
- **WHEN** the package is generated
- **THEN** `.dataspec/field-catalog.schema.json` defines the top-level `projectId`, `fields`, and `enums` properties
- **AND** `.dataspec/field-catalog.json` remains valid JSON using the same top-level property names

#### Scenario: Schema contains personal field metadata
- **WHEN** the AI Context package is generated
- **THEN** `.dataspec/field-catalog.schema.json` allows field aliases, category, codeSetId, sensitive, status, and example

### Requirement: AI Usage Guidance
The AI Context package SHALL include concise guidance for coding agents and prompts.

#### Scenario: Agent can discover usage instructions
- **WHEN** a coding agent reads `AGENTS.md.fragment`
- **THEN** the fragment instructs the agent to read `.dataspec/DATABASE_RULES.md`, `.dataspec/field-catalog.json`, and `.dataspec/rules.yaml` before creating or modifying database schema

#### Scenario: Prompt template explains common workflows
- **WHEN** a user opens `.dataspec/prompts.md`
- **THEN** the file contains prompts for creating a table and reviewing/fixing SQL according to DataSpec rules

### Requirement: Structured Naming Rules Export
The AI rules export SHALL include a structured naming model for AI clients.

#### Scenario: Export naming rules
- **WHEN** rules.yaml is generated
- **THEN** it contains a `naming:` section
- **AND** the section includes case rules, required columns, forbidden names, recommendations, suffix type rules, and prefix type rules

### Requirement: Snapshot-scoped AI Context export
The AI Context package SHALL support exporting context from a specified historical standard snapshot.

#### Scenario: Preview field catalog for snapshot
- **WHEN** a caller previews `field-catalog.json` with `snapshotId`
- **THEN** the response uses the snapshot payload
- **AND** the top-level standard metadata marks the source as `snapshot`

#### Scenario: Preview rules for snapshot
- **WHEN** a caller previews `rules.yaml` with `snapshotId`
- **THEN** the response uses the snapshot rule payload
- **AND** the YAML standard block includes snapshot ID, version, hash, and source

#### Scenario: Download package for snapshot
- **WHEN** a caller downloads the AI Context zip with `snapshotId`
- **THEN** `.dataspec/manifest.json`, `.dataspec/field-catalog.json`, and `.dataspec/rules.yaml` all reference the same snapshot metadata

### Requirement: AI rules export includes baseline metadata
The AI Context `rules.yaml` export SHALL include current project rule baseline metadata without removing existing rule fields.

#### Scenario: Export project with applied baseline
- **WHEN** `rules.yaml` is generated for a project with baseline metadata
- **THEN** the YAML includes baseline key, name, version, source, applied time, and rule count
- **AND** existing `standard`, `naming`, and `rules` sections remain present

#### Scenario: Export project without baseline
- **WHEN** `rules.yaml` is generated for a project without baseline metadata
- **THEN** the YAML identifies the baseline as custom or inferred
- **AND** the export still includes the actual enabled project rules

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

### Requirement: Field catalog exports starter kit source metadata
The AI Context field catalog SHALL include additive starter kit source metadata for fields created from domain starter kits.

#### Scenario: Export field created from starter kit
- **WHEN** AI context field catalog is generated for a project with starter kit fields
- **THEN** each starter kit field includes starterKitSources with kitKey and kitVersion
- **AND** the existing field name, type, tags, aliases, status, sensitive, and example metadata remain compatible.

#### Scenario: Export project without starter kit fields
- **WHEN** AI context field catalog is generated for a project without starter kit source markers
- **THEN** the field catalog remains valid
- **AND** starterKitSources is omitted or empty for those fields.

### Requirement: AI Context package includes capability catalog
The AI Context package SHALL include the DataSpec capability catalog so offline agents can discover supported workflows and surfaces.

#### Scenario: Export package with capabilities
- **WHEN** an AI Context zip is generated
- **THEN** it contains `.dataspec/capabilities.json`
- **AND** manifest files list the capability catalog artifact.

#### Scenario: Capability catalog is documented in package README
- **WHEN** a caller reads the package README or AGENTS fragment
- **THEN** it instructs AI agents to read `.dataspec/capabilities.json` before selecting CLI, MCP, or API actions.

#### Scenario: Offline cache includes capabilities
- **WHEN** CLI export-context writes an offline `.dataspec/context/` cache
- **THEN** the cache includes the capability catalog file from the exported package
- **AND** the file does not contain secrets or business data rows.
