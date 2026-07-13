## ADDED Requirements

### Requirement: Query token evidence uses stable Explain Trace semantics
Explain Trace SHALL represent query-token and business-glossary matching with stable source types and rule codes without changing the existing trace field structure.

#### Scenario: Direct lexical split is explained
- **WHEN** a field result depends on acronym, camel, number, or unit tokenization
- **THEN** its evidence can use `sourceType=QUERY_TOKEN` and `ruleCode=NAME_SPLIT`
- **AND** matchReason identifies the sanitized normalized token and deterministic boundary type.

#### Scenario: Glossary expansion is explained
- **WHEN** a field result depends on a resolved glossary term or abbreviation
- **THEN** its evidence uses `sourceType=BUSINESS_GLOSSARY` and a stable rule code such as `GLOSSARY_LONGEST_MATCH` or `ABBREVIATION_EXPANSION`
- **AND** sourceId identifies the current-project glossary entry when one stable entry exists.

#### Scenario: Ambiguous or disabled token is explained
- **WHEN** normalization reports an ambiguous abbreviation or disabled term
- **THEN** evidence uses `ABBREVIATION_AMBIGUOUS` or `DISABLED_TERM`
- **AND** it does not claim a canonical field confidence.
