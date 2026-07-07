## ADDED Requirements

### Requirement: Field search usage contract evidence
Field standard search SHALL return field usage contract evidence when it helps explain why a field should or should not be used.

#### Scenario: Search result includes usage guidance
- **WHEN** a caller searches fields and a matched field has preferred use cases, avoid conditions, join hints, default filters, aggregation hints, replacement guidance, or misuse examples
- **THEN** each matching search item includes a concise usage contract summary
- **AND** existing field, score, matchReasons, recommendedUse, and nextActions remain compatible

#### Scenario: Search query matches avoid condition
- **WHEN** a search query or structured question matches a field avoid condition or misuse example
- **THEN** the search result includes a next action that requires confirmation before using that field
- **AND** the field is not described as directly safe for that scenario
