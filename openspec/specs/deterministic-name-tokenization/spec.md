# deterministic-name-tokenization Specification

## Purpose
Define deterministic, project-scoped name tokenization and glossary resolution for field search, suggestion, and AI-readable evidence without external models or runtimes.

## Requirements
### Requirement: Names are tokenized deterministically
DataSpec SHALL tokenize names in a stable order across separators, camelCase, acronym boundaries, letter-number boundaries, and bounded unit classification without calling an external model or runtime.

#### Scenario: Tokenize acronym camel and number boundaries
- **WHEN** DataSpec normalizes `HTTPStatus2Code`
- **THEN** the ordered lexical tokens are `http`, `status`, `2`, and `code`
- **AND** repeated normalization returns the same normalized text, token kinds, and evidence order.

#### Scenario: Tokenize separated abbreviations
- **WHEN** DataSpec normalizes `ord_amt`
- **THEN** `ord` and `amt` remain separate abbreviation candidates
- **AND** DataSpec does not invent canonical expansions without current-project glossary evidence.

### Requirement: Project glossary matching is longest and conservative
DataSpec SHALL use only enabled glossary entries from the selected project, choose deterministic longest matches, and refuse to guess when one normalized abbreviation maps to different canonical fields.

#### Scenario: Longest Chinese glossary term wins
- **WHEN** the current project contains overlapping enabled terms for `会员`, `手机号`, and `会员手机号`, and the query contains `会员手机号`
- **THEN** normalization selects the longest `会员手机号` match at that position
- **AND** token evidence identifies its glossary source and canonical field.

#### Scenario: Abbreviation is ambiguous
- **WHEN** an exact lexical abbreviation maps to different canonical fields in enabled current-project glossary entries
- **THEN** its resolution status is `AMBIGUOUS`
- **AND** DataSpec does not select a canonical term or field for that token.

#### Scenario: Disabled term is encountered
- **WHEN** a query matches a current-project disabled term
- **THEN** its resolution status is `DISABLED`
- **AND** it cannot become a high-confidence canonical match.

### Requirement: Token evidence is bounded and secret-safe
DataSpec SHALL bound token count, token length, candidate identifiers, and explanation length, and SHALL redact secret-like query content before returning token evidence.

#### Scenario: Query contains secret-like text
- **WHEN** a query token contains a token, password, Authorization, JDBC URL, DSN, or connection-string-like value
- **THEN** search, suggestion, Standard Query, frontend summaries, logs, and diagnostics do not expose the raw value
- **AND** deterministic matching either uses safe internal normalization or reports a redacted unresolved token.
