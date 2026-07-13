## ADDED Requirements

### Requirement: FIELD text exposes shared token normalization
Standard Query DSL SHALL execute FIELD text through field search's deterministic name normalization and SHALL expose the same additive query token evidence in the normalized query result.

#### Scenario: DSL and legacy search receive the same text
- **WHEN** Standard Query FIELD search and legacy field search run the same project and text
- **THEN** they use the same token order, glossary resolution statuses, and canonical evidence
- **AND** their field result ordering is not changed by a second independent tokenizer.

#### Scenario: Explain is disabled
- **WHEN** a Standard Query request sets `explain=false`
- **THEN** deterministic tokenization still governs matching
- **AND** token evidence may be omitted from the response while existing normalized fields remain compatible.
