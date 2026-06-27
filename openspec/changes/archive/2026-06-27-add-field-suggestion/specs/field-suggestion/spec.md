## ADDED Requirements

### Requirement: Field Suggestion API
The system SHALL recommend standard field candidates for a business description in a project.

#### Scenario: Suggest existing field by alias or description
- **WHEN** a client requests field suggestions with `projectId` and `query`
- **THEN** the system returns ranked candidates from the project's standard fields
- **AND** each candidate includes the field, score, match reason, recommended field name, and whether it already exists

#### Scenario: Exclude disabled fields
- **WHEN** a field is marked `disabled`
- **THEN** it is not returned as an existing field suggestion

#### Scenario: Unknown query fallback
- **WHEN** no existing field is a meaningful match
- **THEN** the system returns a fallback candidate with a generated snake_case `recommendedName`
- **AND** `existing` is false

### Requirement: CLI Field Suggestion
The CLI SHALL expose field suggestion as a JSON command for AI and CI workflows.

#### Scenario: Suggest field via CLI
- **WHEN** a user runs `suggest-field <query> --project <id> --format json`
- **THEN** the CLI calls the field suggestion API and prints the JSON result

### Requirement: MCP Field Suggestion Tool
The MCP server SHALL expose field suggestion as a tool.

#### Scenario: Suggest fields via MCP
- **WHEN** an MCP client calls `suggest_fields` with `query`
- **THEN** the server calls the field suggestion API and returns structured JSON content
