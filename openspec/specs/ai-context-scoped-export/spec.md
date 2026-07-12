# ai-context-scoped-export Specification

## Purpose
定义 AI Context 和字段目录的按需裁剪导出能力，通过字段、领域、影响范围和数量限制减少上下文体积，同时保留可解释的 scope metadata。
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

### Requirement: Budget-aware AI Context export preview
AI Context scoped export SHALL expose budget planning as a pre-export preview without changing existing export semantics.

#### Scenario: Preview budget before export
- **WHEN** a frontend or API caller prepares an AI Context export with scope, query, status, limit, profile, task type, or token budget
- **THEN** DataSpec can return a budget plan using the same effective scope semantics as scoped export.
- **AND** the existing field catalog preview, package download, and cache export behavior remain compatible when no budget plan is requested.

#### Scenario: Recommended export params remain advisory
- **WHEN** the budget plan returns `recommendedExportParams`
- **THEN** the frontend may show or apply those parameters only as an explicit user action.
- **AND** DataSpec MUST NOT silently override existing export parameters solely because the planner produced a recommendation.

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
