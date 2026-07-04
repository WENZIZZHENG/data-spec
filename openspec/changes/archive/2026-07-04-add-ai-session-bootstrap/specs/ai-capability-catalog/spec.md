## ADDED Requirements

### Requirement: Capability catalog includes session bootstrap
The AI capability catalog SHALL describe the session bootstrap entry point.

#### Scenario: List session bootstrap capability
- **WHEN** a caller lists the AI capability catalog
- **THEN** the catalog includes a stable `session-bootstrap` capability
- **AND** the capability lists API, CLI, and MCP surfaces for reading the AI session bootstrap package.

#### Scenario: Bootstrap capability safety
- **WHEN** the catalog describes `session-bootstrap`
- **THEN** it marks the capability as read-only
- **AND** its preflight checks and next actions explain that the bootstrap does not execute lint, export context, reverse import, DDL generation, or writes.
