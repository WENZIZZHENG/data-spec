## ADDED Requirements

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
