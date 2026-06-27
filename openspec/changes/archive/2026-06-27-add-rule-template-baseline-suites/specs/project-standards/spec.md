## ADDED Requirements

### Requirement: Project creation can apply default rule baseline
Project initialization SHALL be able to apply the personal default rule baseline without requiring manual rule setup.

#### Scenario: Create project with built-in standards
- **WHEN** a project is created with built-in standards enabled
- **THEN** DataSpec can apply the `personal_default` rule baseline for that project
- **AND** existing built-in field and domain initialization behavior remains unchanged

#### Scenario: Create blank project
- **WHEN** a project is created without built-in standards
- **THEN** DataSpec does not force a rule baseline
- **AND** the user can still apply a baseline later from the rule config page
