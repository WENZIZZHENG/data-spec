## ADDED Requirements

### Requirement: Field search semantic card evidence
Field standard search SHALL use semantic rules, naming translations, enum lifecycle, and knowledge card summaries when explaining search results.

#### Scenario: Search uses preferred and forbidden translations
- **WHEN** a user or AI searches with a localized term, translation alias, preferred English name, or forbidden translation
- **THEN** field search includes translation match reasons and warnings where applicable
- **AND** forbidden translations do not boost a field as a direct safe recommendation.

#### Scenario: Search result includes semantic summary
- **WHEN** a matching field has semantic rules, enum lifecycle hints, metric references, or knowledge card risk notes
- **THEN** each search item includes a concise semantic summary or next action
- **AND** existing field, score, matchReasons, recommendedUse, usageContractSummary, evidence, stableRef, canonicalRef, lifecycleStatus, and matchedAlias remain compatible.

#### Scenario: Search avoids noisy full knowledge card dumps
- **WHEN** search returns multiple fields
- **THEN** DataSpec returns bounded card summaries and links or ids for detail lookup rather than embedding every full card.
