# prompt-template-registry Specification

## Purpose
Provide stable version metadata, output constraints, and deterministic local evaluation for AI-facing Prompt and generation templates.

## Requirements
### Requirement: Prompt template registry
The system SHALL maintain a registry of AI-facing prompt and generation templates with stable version metadata.

#### Scenario: List template definitions
- **WHEN** a caller lists prompt templates
- **THEN** each definition includes templateKey, promptVersion, scenario, title, outputFormat, requiredSections, requiredPhrases, and changeLog.
- **AND** promptVersion uses a stable `<templateKey>@<version>` format.

#### Scenario: Resolve template version
- **WHEN** a service records an AI job for create-table prompt, fix-sql prompt, SQL lint/fixedSql, or DDL preview
- **THEN** the service resolves the promptVersion from the registry instead of hard-coded local strings.

### Requirement: Prompt template evaluation
The system SHALL provide a deterministic local evaluation for registered prompt templates.

#### Scenario: Evaluate generated prompt text
- **WHEN** a generated prompt is evaluated for a registered template
- **THEN** the result reports PASS only if every required section and required phrase is present.
- **AND** the result includes readable failures when a required marker is missing.

#### Scenario: Golden prompt fixture changes
- **WHEN** a registered prompt output differs from its golden fixture
- **THEN** the verification output includes a readable diff showing removed and added lines.

#### Scenario: Evaluation runs without external LLM
- **WHEN** a developer runs the existing backend test entrance
- **THEN** prompt template evaluation runs without requiring network access, third-party API keys, or external model calls.

### Requirement: Prompt template API
The system SHALL expose prompt template metadata and evaluation results through backend APIs for AI or future frontend clients.

#### Scenario: Prompt template metadata API
- **WHEN** a client requests prompt template metadata
- **THEN** the response returns all registered template definitions without large generated prompt payloads.

#### Scenario: Prompt template evaluation API
- **WHEN** a client submits a templateKey and output text for evaluation
- **THEN** the response returns templateKey, promptVersion, passed, failures, requiredSections, and requiredPhrases.
