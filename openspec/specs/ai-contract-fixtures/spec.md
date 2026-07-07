# ai-contract-fixtures Specification

## Purpose
TBD - created by archiving change add-ai-contract-fixtures. Update Purpose after archive.
## Requirements
### Requirement: AI contracts have golden coverage
The system SHALL provide contract fixtures or golden assertions for the primary AI-consumed outputs.

#### Scenario: Backend AI contract output changes
- **WHEN** backend code changes AI Context, SQL lint, fixed SQL, field suggestion, or DDL preview output
- **THEN** existing backend tests detect missing or incompatible stable fields.

#### Scenario: CLI and MCP AI contract output changes
- **WHEN** CLI JSON output or MCP resource/tool output changes
- **THEN** existing Node tests detect missing or incompatible stable fields.

### Requirement: Fixtures cover stable field paths
The contract tests SHALL verify stable field paths, value types, and stable enum values rather than relying only on broad text contains checks.

#### Scenario: Stable field is removed or renamed
- **WHEN** a stable AI-consumed field such as `ruleCode`, `severity`, `fixedSql`, `recommendedName`, `lintResult`, `schemaVersion`, or `recipes` is removed or renamed
- **THEN** contract tests fail with a readable assertion.

#### Scenario: Compatible field is added
- **WHEN** an output adds a new optional field without changing existing stable fields
- **THEN** contract tests continue to pass unless the new field changes documented semantics.

### Requirement: Compatibility policy is documented
The system SHALL document which AI-consumed fields are stable and how compatible additions should be handled.

#### Scenario: AI agent reads compatibility guidance
- **WHEN** a developer or AI agent reads the project documentation
- **THEN** it can identify stable AI contract outputs and understand that additive fields are compatible while removal, rename, or semantic changes require fixture updates.

### Requirement: Contract tests use existing validation entrances
The contract fixtures MUST be executed through the existing project validation commands.

#### Scenario: Developer runs standard validation
- **WHEN** a developer runs `mvn test` or `node --test` for the affected module
- **THEN** the AI contract fixture checks are included without requiring a separate external service.

### Requirement: Prompt template contracts have fixture coverage
The system SHALL verify prompt template registry and prompt output constraints through existing backend validation.

#### Scenario: Prompt template metadata changes
- **WHEN** a prompt template key, version, required section, required phrase, or output format changes
- **THEN** backend tests detect incompatible or incomplete registry metadata.

#### Scenario: Prompt output contract changes
- **WHEN** generated create-table or fix-sql prompt output loses a required section, required phrase, or promptVersion marker
- **THEN** backend tests fail with a readable assertion.

#### Scenario: Golden prompt output changes
- **WHEN** generated prompt text changes from the checked-in golden fixture
- **THEN** backend tests fail and report a readable diff for review.

### Requirement: Contract fixtures cover schema registry
The existing AI contract fixture checks SHALL cover the schema registry and registry consumers.

#### Scenario: Registry contract is incomplete
- **WHEN** a required core contract id, `schemaVersion`, `stableFields`, `jsonSchema`, or compatibility policy is missing
- **THEN** backend contract tests fail with a readable assertion.

#### Scenario: AI Context registry metadata drifts
- **WHEN** AI Context package manifest or `.dataspec/schema-registry.json` loses registry metadata
- **THEN** backend AI Context tests fail.

#### Scenario: CLI and MCP registry output drifts
- **WHEN** CLI contract commands or MCP schema registry resource lose stable registry fields
- **THEN** Node contract tests fail.

### Requirement: Evidence package contract fixtures
The AI contract fixture checks SHALL cover evidence package stable fields and redaction.

#### Scenario: Evidence stable fields drift
- **WHEN** the evidence package loses `kind`, `schemaVersion`, `source`, `standardSnapshot`, `validationSummary`, `artifacts`, `nextActions`, or `suggestedCommands`
- **THEN** backend or Node contract tests fail with a readable assertion.

#### Scenario: Evidence redaction drifts
- **WHEN** an evidence package source contains token, password, Authorization header, or complete JDBC URL values
- **THEN** tests fail if any of those raw sensitive values appear in JSON, Markdown, zip contents, CLI output, or MCP output.

#### Scenario: Evidence zip drifts
- **WHEN** evidence zip generation loses `evidence.json`, `summary.md`, or `README.md`
- **THEN** backend tests fail.

### Requirement: Status check summarizes issue codes
The local status check JSON output SHALL include issue code summaries so AI agents can distinguish repeated instances of the same problem type from distinct problem types.

#### Scenario: Repeated warning code is summarized
- **WHEN** the status check emits multiple warning issues with the same code
- **THEN** `summary.issueCodes[]` contains one item for that code.
- **AND** that item includes the total count and `severity` equal to `warning`.

#### Scenario: Error code is summarized
- **WHEN** the status check emits error issues
- **THEN** `summary.issueCodes[]` contains the error code with its count.
- **AND** that item reports `severity` equal to `error`.

#### Scenario: Existing issue details remain authoritative
- **WHEN** issue code summaries are emitted
- **THEN** existing `issues[]`, `checks[]`, `summary.errors`, `summary.warnings`, `status`, and exit code semantics remain unchanged.
