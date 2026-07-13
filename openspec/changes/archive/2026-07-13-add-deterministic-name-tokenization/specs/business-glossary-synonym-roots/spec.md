## ADDED Requirements

### Requirement: Glossary matching follows shared tokenization
Business glossary matching SHALL use the shared deterministic lexical tokens and current-project enabled entries for term, synonym, root, abbreviation, and disabled-term resolution.

#### Scenario: Multi-token glossary phrase is matched
- **WHEN** an enabled glossary phrase spans consecutive lexical or Chinese dictionary tokens
- **THEN** DataSpec uses the longest match before considering shorter overlapping entries
- **AND** the match preserves term type, glossary ids, canonical field, and an auditable reason.

#### Scenario: Abbreviation requires an exact token
- **WHEN** a configured abbreviation is only a substring of a longer lexical token
- **THEN** DataSpec does not expand it
- **AND** the same abbreviation expands when it appears as a complete token and has one canonical meaning.

#### Scenario: Conflicting abbreviation remains unresolved
- **WHEN** the same normalized abbreviation belongs to enabled entries with different canonical fields
- **THEN** glossary matching returns an ambiguous resolution with candidate ids
- **AND** no candidate receives the normal abbreviation match score.
