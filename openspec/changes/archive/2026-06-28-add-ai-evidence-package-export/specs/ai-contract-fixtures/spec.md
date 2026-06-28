## ADDED Requirements

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
