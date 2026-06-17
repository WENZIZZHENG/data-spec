## ADDED Requirements

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

### Requirement: AI Usage Guidance
The AI Context package SHALL include concise guidance for coding agents and prompts.

#### Scenario: Agent can discover usage instructions
- **WHEN** a coding agent reads `AGENTS.md.fragment`
- **THEN** the fragment instructs the agent to read `.dataspec/DATABASE_RULES.md`, `.dataspec/field-catalog.json`, and `.dataspec/rules.yaml` before creating or modifying database schema

#### Scenario: Prompt template explains common workflows
- **WHEN** a user opens `.dataspec/prompts.md`
- **THEN** the file contains prompts for creating a table and reviewing/fixing SQL according to DataSpec rules
