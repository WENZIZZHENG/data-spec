## ADDED Requirements

### Requirement: DDL preview prompt template version
DDL generation SHALL record its AI replay promptVersion from the prompt template registry.

#### Scenario: DDL replay references registry version
- **WHEN** a user previews DDL from a table template
- **THEN** the AI job record promptVersion matches the DDL preview template registered in the prompt template registry.
