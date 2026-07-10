## ADDED Requirements

### Requirement: Evidence packages carry post-check summaries
AI evidence packages SHALL be able to include stable reference and post-check summaries without storing raw AI output or secrets.

#### Scenario: Evidence includes validation summary
- **WHEN** an AI task or payload has a post-check result
- **THEN** the evidence package SHALL include status, safeToUse, issue counts, blocking refs, replacement refs, evidence links, and suggested check command
- **AND** existing evidence package fields SHALL remain compatible.

#### Scenario: Evidence excludes unsafe output
- **WHEN** post-check input or issues contain raw AI output, token, password, Authorization, JDBC URL, DSN, or business data rows
- **THEN** the evidence package SHALL store only bounded redacted excerpts and structured reference summaries.
