# ai-context-scoped-export Specification

## Purpose
TBD - created by archiving change add-ai-context-scoped-export. Update Purpose after archive.
## Requirements
### Requirement: Scoped field catalog export
The system SHALL support optional scope filters when exporting the AI field catalog.

#### Scenario: Default full catalog remains compatible
- **WHEN** a caller exports the field catalog without scope parameters
- **THEN** the system returns all project fields
- **AND** the top-level `projectId`, `standard`, `fields`, and `enums` properties remain available.

#### Scenario: Field text scope
- **WHEN** a caller exports with `scope=field` and a non-empty `query`
- **THEN** the system returns only fields whose name, display name, aliases, comment, category, type, status, or example match the query text
- **AND** each returned field includes match reasons.

#### Scenario: Domain scope
- **WHEN** a caller exports with `scope=domain` and a non-empty `query`
- **THEN** the system returns fields whose category or related text matches the query
- **AND** the `contextScope` metadata records the scope, query, matched field count, total field count, and warnings.

#### Scenario: Limit truncation
- **WHEN** scoped export matches more fields than `limit`
- **THEN** the system returns at most `limit` fields
- **AND** the metadata includes a warning that results were truncated.

### Requirement: Scoped AI Context package
The system SHALL support optional scope filters when downloading the AI Context package.

#### Scenario: Package includes scope metadata
- **WHEN** a caller downloads the AI Context package with scope parameters
- **THEN** `.dataspec/field-catalog.json` contains the scoped field list and `contextScope`
- **AND** `.dataspec/manifest.json` contains the same scope summary
- **AND** `.dataspec/README.md` explains when to use full or scoped context.

### Requirement: CLI scoped export
The CLI SHALL allow users to export a scoped AI Context package.

#### Scenario: Export with scope options
- **WHEN** a user runs `dataspec export-context --scope field --query 手机 --limit 20 --output ctx.zip`
- **THEN** the CLI requests the package download endpoint with matching query parameters
- **AND** the downloaded bytes are written to the requested output path.

### Requirement: MCP retrieval for current tasks
The MCP server SHALL provide a retrieval-oriented field catalog tool.

#### Scenario: Search field catalog
- **WHEN** an MCP client calls `search_field_catalog` with a query
- **THEN** the server reads the scoped field catalog endpoint
- **AND** returns structured JSON containing the scoped fields and metadata.

### Requirement: Frontend scoped AI Context
The frontend SHALL let users preview and download scoped AI Context from the AI Context page.

#### Scenario: Preview scoped catalog
- **WHEN** a user selects a scope and enters a query
- **THEN** the field catalog preview uses those filters
- **AND** the download button exports a package with the same filters.

### Requirement: Profile-driven scoped context
AI Context export SHALL support using an AI task profile as the source of scoped export defaults.

#### Scenario: Profile supplies context scope
- **WHEN** a client exports AI Context with a profile and no explicit scope options
- **THEN** DataSpec applies the profile's context scope, status, query, and limit defaults where defined.

#### Scenario: Explicit scope wins
- **WHEN** a client exports AI Context with both a profile and explicit scope options
- **THEN** the explicit scope options take precedence over the profile defaults.

#### Scenario: Manifest records profile
- **WHEN** a profile influences the exported AI Context package
- **THEN** the package manifest records the profile id, task type, and effective context scope.

### Requirement: Scoped Usage Example Export
Scoped AI Context export SHALL apply scope and query filters to usage examples.

#### Scenario: Field scope filters examples
- **WHEN** a caller exports AI Context with `scope=field` and a query that matches a subset of standard fields
- **THEN** `.dataspec/usage-examples.json` includes examples tied to those matched fields first
- **AND** unrelated field examples are omitted unless they are `GENERAL` examples matching the query.

#### Scenario: Limit truncates examples
- **WHEN** more enabled examples match than the usage example export limit
- **THEN** the export returns at most that limit
- **AND** the summary records that examples were truncated.

#### Scenario: Snapshot export remains stable
- **WHEN** a caller exports AI Context for a historical snapshot
- **THEN** usage examples remain project-scoped current metadata
- **AND** the manifest or usage examples file records that examples are not part of the snapshot payload.
