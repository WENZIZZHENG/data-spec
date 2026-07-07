## ADDED Requirements

### Requirement: DDL and prompt context respects field usage contracts
DDL and AI prompt generation SHALL include field usage contract guidance when standard field context is available.

#### Scenario: Prompt guidance includes usage boundaries
- **WHEN** DDL generation or prompt guidance includes standard field context for fields with usage contracts
- **THEN** the context includes preferred use cases, avoid conditions, join hints, default filters, aggregation hints, replacement guidance, or misuse examples as applicable
- **AND** the guidance tells AI clients not to use fields in avoid conditions without explicit human confirmation

#### Scenario: Usage contract does not mutate generated SQL
- **WHEN** a usage contract is available for a field
- **THEN** DataSpec treats it as read-only guidance for DDL and prompt generation
- **AND** it does not automatically rewrite user SQL, apply database migrations, or modify project fields
