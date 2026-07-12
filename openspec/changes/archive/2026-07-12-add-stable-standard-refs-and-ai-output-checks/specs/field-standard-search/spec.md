## ADDED Requirements

### Requirement: Field search returns stable reference evidence
Field standard search SHALL return stable references and explain historical-name resolution.

#### Scenario: Search result includes stable refs
- **WHEN** field search returns a match
- **THEN** the item SHALL include `stableRef`, `canonicalRef`, and lifecycle status
- **AND** existing score, matchReasons, recommendedUse, usage contract, and nextActions SHALL remain compatible.

#### Scenario: Search matches alias history
- **WHEN** a query matches an alias or historical field name
- **THEN** search SHALL identify the matched alias or historical ref
- **AND** it SHALL return the current field stableRef and replacement warning when applicable.
