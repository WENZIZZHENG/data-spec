## ADDED Requirements

### Requirement: Field suggestion naming and semantic guidance
Field suggestion SHALL use naming translation guidance and field semantic rules to improve deterministic recommendations.

#### Scenario: Suggest canonical field from translation guidance
- **WHEN** a suggestion query contains a Chinese term, English alias, or translation alias maintained on a standard field
- **THEN** DataSpec ranks the canonical standard field ahead of generic fallback names
- **AND** the match reason explains the translation guidance source.

#### Scenario: Suggest fallback avoids forbidden translation
- **WHEN** no existing field is a meaningful match but the query contains a forbidden translation
- **THEN** the fallback candidate avoids that forbidden name
- **AND** next actions explain which preferred English name or canonical field should be considered.

#### Scenario: Suggestion includes semantic caution
- **WHEN** a matching field has source-of-truth, unit conversion, enum lifecycle, or metric-boundary warnings
- **THEN** DataSpec includes a concise caution in match reason or next actions without changing the existing suggestion response shape.
