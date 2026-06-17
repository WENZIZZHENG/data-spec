## MODIFIED Requirements

### Requirement: Structured Naming Rules Export
The AI rules export SHALL include a structured naming model for AI clients.

#### Scenario: Export naming rules
- **WHEN** rules.yaml is generated
- **THEN** it contains a `naming:` section
- **AND** the section includes case rules, required columns, forbidden names, recommendations, suffix type rules, and prefix type rules
