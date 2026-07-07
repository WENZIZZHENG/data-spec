# ai-context-package Specification

## Purpose
定义项目级 AI Context zip 包的下载契约、稳定文件布局和字段目录 schema，使 AI 能离线读取 DataSpec 规则、字段标准、示例和 Agent 指令片段。
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

#### Scenario: Schema contains field format constraints
- **WHEN** the AI Context package is generated
- **THEN** `.dataspec/field-catalog.schema.json` allows each field to include a `format` object with type, pattern, unit, precision, timezone, nullPolicy, validExamples, invalidExamples, and notes.
- **AND** `validExamples` and `invalidExamples` are arrays of strings.

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

### Requirement: AI Context Field Format Export
The AI Context package SHALL export field value-format constraints in AI-readable context.

#### Scenario: Field catalog exports format constraints
- **WHEN** a field has format constraints
- **THEN** `.dataspec/field-catalog.json` SHALL include a `format` object for that field.
- **AND** the object SHALL preserve unit, timezone, precision, valid examples, invalid examples, and notes without exposing business data rows.

#### Scenario: Database rules mention format constraints
- **WHEN** `DATABASE_RULES.md` is generated
- **THEN** fields with format constraints SHALL include a concise value-format line or inline summary.
- **AND** create-table/fix-sql prompts that embed AI Context SHALL allow the AI to read these constraints before generating DDL or SQL fixes.

### Requirement: AI Context field naming risk export
The AI Context package SHALL export concise field naming risks for AI clients.

#### Scenario: Export naming risks
- **WHEN** a project has field conflict groups for reserved words, dangerous SQL names, case collisions, or ambiguous aliases
- **THEN** `.dataspec/DATABASE_RULES.md` includes a concise naming risk section with conflict type, field names, evidence, and suggested action.
- **AND** the section tells AI clients to avoid using risky names directly for new DDL unless explicitly required.

#### Scenario: No naming risks
- **WHEN** a project has no naming risk conflict groups
- **THEN** AI Context generation continues without adding empty or noisy naming risk content.

### Requirement: AI Context Package Includes Usage Examples
The AI Context package SHALL include concise standard usage examples and anti-examples for AI clients.

#### Scenario: Package contains usage examples file
- **WHEN** an AI Context zip is generated for a project
- **THEN** it contains `.dataspec/usage-examples.json`
- **AND** that file contains `projectId`, `schemaVersion`, `contextScope`, `examples`, and `summary`.

#### Scenario: Field catalog references usage examples
- **WHEN** `field-catalog.json` is generated
- **THEN** the top-level JSON includes additive `usageExamples` and `usageExampleSummary` properties
- **AND** existing `projectId`, `standard`, `fields`, and `enums` properties remain present.

#### Scenario: Package guidance mentions examples
- **WHEN** a caller reads `.dataspec/README.md` or `AGENTS.md.fragment`
- **THEN** the guidance tells AI clients to prefer `GOOD` examples and avoid `BAD` examples with matching scope.

#### Scenario: No examples remain compatible
- **WHEN** a project has no enabled usage examples
- **THEN** the package still contains a valid `.dataspec/usage-examples.json` with an empty `examples` array
- **AND** existing package files remain generated.

### Requirement: AI Context Field Usage Contract Export
The AI Context package SHALL export field usage contracts in machine-readable and human-readable forms.

#### Scenario: Field catalog exports usage contract
- **WHEN** a field has one or more usage contract values
- **THEN** `.dataspec/field-catalog.json` includes a `usageContract` object for that field
- **AND** that object can include preferred use cases, avoid conditions, join hints, default filters, aggregation hints, replacement guidance, and misuse examples
- **AND** `.dataspec/field-catalog.schema.json` describes each usage contract property

#### Scenario: Database rules mention usage boundaries
- **WHEN** `DATABASE_RULES.md` is generated for fields with usage contracts
- **THEN** it includes concise field usage guidance for high-risk or scoped fields
- **AND** it tells AI clients to respect avoid conditions before generating SQL or DDL

#### Scenario: No usage contracts remain compatible
- **WHEN** a project has no field usage contract values
- **THEN** AI Context files remain valid
- **AND** empty usage contract sections are omitted rather than emitted as noisy placeholders
