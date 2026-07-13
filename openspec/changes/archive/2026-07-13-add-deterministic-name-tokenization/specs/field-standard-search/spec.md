## ADDED Requirements

### Requirement: Field search explains deterministic query normalization
Field standard search SHALL use the same deterministic query normalization as field suggestion and SHALL expose additive bounded query token evidence in the search summary and matching field evidence.

#### Scenario: Search an acronym and number name
- **WHEN** a caller searches for `HTTPStatus2Code`
- **THEN** search evaluates the ordered `http`, `status`, `2`, and `code` tokens rather than only `httpstatus2` and `code`
- **AND** the summary explains direct, resolved, unresolved, or unit token states.

#### Scenario: Search a Chinese glossary phrase
- **WHEN** a query contains a longest current-project glossary match such as `会员手机号`
- **THEN** fields bound to that canonical glossary meaning receive deterministic glossary evidence
- **AND** shorter overlapping terms do not independently create a competing high-confidence match at the same position.

#### Scenario: Search reports token ambiguity
- **WHEN** normalization finds an ambiguous or disabled token
- **THEN** search summary hints and query token evidence expose the conservative resolution
- **AND** no field is ranked from a guessed canonical expansion.
