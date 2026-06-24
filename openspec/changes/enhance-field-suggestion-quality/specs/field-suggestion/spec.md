## ADDED Requirements

### Requirement: Semantic Field Suggestion Quality
The field suggestion API SHALL use deterministic semantic matching to improve recommendations for common personal and small-team database field descriptions without calling an external LLM.

#### Scenario: Recommend by Chinese synonym or pinyin abbreviation
- **WHEN** a client requests suggestions with a Chinese synonym, English alias, or pinyin abbreviation for a known semantic group
- **THEN** the system returns matching standard fields ranked ahead of less specific generic fields
- **AND** the match reason explains the semantic keyword that caused the recommendation

#### Scenario: Penalize generic-only matches
- **WHEN** a query only shares generic business words such as user, order, amount, status, time, or date with a field
- **THEN** the system gives that candidate a lower score than candidates that match a more specific semantic keyword

#### Scenario: Sensitive field explanation
- **WHEN** a recommended existing field is marked sensitive
- **THEN** its match reason includes a sensitive-field hint while keeping the existing response structure compatible

#### Scenario: Standard fallback names
- **WHEN** no existing field is a meaningful match but the query contains a known semantic group
- **THEN** the fallback candidate uses the canonical standard snake_case name for that semantic group
- **AND** the fallback remains marked as `existing=false`
