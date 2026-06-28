# ai-prompt-generator Specification

## Purpose
TBD - created by archiving change add-ai-prompt-generator. Update Purpose after archive.
## Requirements
### Requirement: Create Table Prompt
The system SHALL generate a plain-text prompt for creating database tables with DataSpec standards, and the prompt SHALL include the registry promptVersion used for generation.

#### Scenario: Generate create table prompt
- **WHEN** a user provides a project ID and business description
- **THEN** the prompt includes the business description, field catalog, naming rules, SQL output requirements, and promptVersion
- **AND** the promptVersion matches the create-table template registered in the prompt template registry

### Requirement: Fix SQL Prompt
The system SHALL generate a plain-text prompt for fixing SQL based on DataSpec lint results, and the prompt SHALL include the registry promptVersion used for generation.

#### Scenario: Generate fix SQL prompt
- **WHEN** a user provides a project ID and SQL
- **THEN** the system lints the SQL
- **AND** the prompt includes the original SQL, lint issues, field catalog, correction requirements, and promptVersion
- **AND** the promptVersion matches the fix-sql template registered in the prompt template registry

### Requirement: Prompt API
The system SHALL expose prompt generation through backend APIs without changing the existing string response contract.

#### Scenario: Create table prompt API
- **WHEN** a client posts to `/api/ai-context/prompts/create-table`
- **THEN** the response contains the generated prompt text
- **AND** the prompt text contains the registry promptVersion

#### Scenario: Fix SQL prompt API
- **WHEN** a client posts to `/api/ai-context/prompts/fix-sql`
- **THEN** the response contains the generated prompt text
- **AND** the prompt text contains the registry promptVersion
