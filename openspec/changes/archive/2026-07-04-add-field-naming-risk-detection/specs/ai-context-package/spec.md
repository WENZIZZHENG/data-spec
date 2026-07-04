## ADDED Requirements

### Requirement: AI Context field naming risk export
The AI Context package SHALL export concise field naming risks for AI clients.

#### Scenario: Export naming risks
- **WHEN** a project has field conflict groups for reserved words, dangerous SQL names, case collisions, or ambiguous aliases
- **THEN** `.dataspec/DATABASE_RULES.md` includes a concise naming risk section with conflict type, field names, evidence, and suggested action.
- **AND** the section tells AI clients to avoid using risky names directly for new DDL unless explicitly required.

#### Scenario: No naming risks
- **WHEN** a project has no naming risk conflict groups
- **THEN** AI Context generation continues without adding empty or noisy naming risk content.
