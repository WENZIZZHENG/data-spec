## ADDED Requirements

### Requirement: Field suggestion uses shared query tokens
Field suggestion SHALL normalize the query once through deterministic name tokenization and SHALL use the resulting direct tokens, resolved glossary expansions, and historical evidence consistently for scoring and fallback.

#### Scenario: Suggest from resolved abbreviation
- **WHEN** `ord_amt` contains exact abbreviations that current-project glossary entries resolve without ambiguity
- **THEN** suggestions use the configured canonical fields or terms instead of treating `ord_amt` as one unknown name
- **AND** each suggestion exposes additive query token evidence and the glossary source.

#### Scenario: Ambiguous abbreviation cannot choose a field
- **WHEN** an abbreviation token is ambiguous or disabled
- **THEN** suggestion does not promote a glossary canonical field from that token
- **AND** fallback reason and token evidence request confirmation or glossary correction.

#### Scenario: Existing direct match keeps priority
- **WHEN** an existing field current name or current alias matches directly while another field only matches an expanded token
- **THEN** the existing direct match keeps its current higher priority when other inputs are equal.
