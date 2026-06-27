## ADDED Requirements

### Requirement: AI rules export includes baseline metadata
The AI Context `rules.yaml` export SHALL include current project rule baseline metadata without removing existing rule fields.

#### Scenario: Export project with applied baseline
- **WHEN** `rules.yaml` is generated for a project with baseline metadata
- **THEN** the YAML includes baseline key, name, version, source, applied time, and rule count
- **AND** existing `standard`, `naming`, and `rules` sections remain present

#### Scenario: Export project without baseline
- **WHEN** `rules.yaml` is generated for a project without baseline metadata
- **THEN** the YAML identifies the baseline as custom or inferred
- **AND** the export still includes the actual enabled project rules
