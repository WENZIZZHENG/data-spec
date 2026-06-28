## ADDED Requirements

### Requirement: Rule change what-if preview
The rule configuration experience SHALL expose a save-before-preview for rule config updates and toggles.

#### Scenario: Edit rule before save
- **WHEN** a user edits rule severity, enabled state, name, or params
- **THEN** the frontend can request a what-if preview and show the SQL lint and AI Context impact summary before saving.

#### Scenario: Toggle rule before save
- **WHEN** a user toggles a rule enabled state
- **THEN** the frontend can show a what-if preview before applying the toggle.
