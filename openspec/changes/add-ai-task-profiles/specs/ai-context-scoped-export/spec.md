## ADDED Requirements

### Requirement: Profile-driven scoped context
AI Context export SHALL support using an AI task profile as the source of scoped export defaults.

#### Scenario: Profile supplies context scope
- **WHEN** a client exports AI Context with a profile and no explicit scope options
- **THEN** DataSpec applies the profile's context scope, status, query, and limit defaults where defined.

#### Scenario: Explicit scope wins
- **WHEN** a client exports AI Context with both a profile and explicit scope options
- **THEN** the explicit scope options take precedence over the profile defaults.

#### Scenario: Manifest records profile
- **WHEN** a profile influences the exported AI Context package
- **THEN** the package manifest records the profile id, task type, and effective context scope.
