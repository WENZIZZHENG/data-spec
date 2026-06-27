# ai-prompt-generator Specification

## Purpose
TBD - created by archiving change add-ai-prompt-generator. Update Purpose after archive.
## Requirements
### Requirement: Create Table Prompt
The system SHALL generate a plain-text prompt for creating database tables with DataSpec standards.

#### Scenario: Generate create table prompt
- **WHEN** a user provides a project ID and business description
- **THEN** the prompt includes the business description, field catalog, naming rules, and SQL output requirements

### Requirement: Fix SQL Prompt
The system SHALL generate a plain-text prompt for fixing SQL based on DataSpec lint results.

#### Scenario: Generate fix SQL prompt
- **WHEN** a user provides a project ID and SQL
- **THEN** the system lints the SQL
- **AND** the prompt includes the original SQL, lint issues, field catalog, and correction requirements

### Requirement: Prompt API
The system SHALL expose prompt generation through backend APIs.

#### Scenario: Create table prompt API
- **WHEN** a client posts to `/api/ai-context/prompts/create-table`
- **THEN** the response contains the generated prompt text

#### Scenario: Fix SQL prompt API
- **WHEN** a client posts to `/api/ai-context/prompts/fix-sql`
- **THEN** the response contains the generated prompt text
