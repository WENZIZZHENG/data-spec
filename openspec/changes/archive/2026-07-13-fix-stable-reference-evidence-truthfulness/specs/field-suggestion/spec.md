## ADDED Requirements

### Requirement: Field suggestion can explain historical-name recall
Field suggestion SHALL use auditable project-scoped field history to recall a current enabled standard field without recommending a historical name as the new canonical name.

#### Scenario: Suggest current field from historical name
- **WHEN** a suggestion query matches a historical name for a current enabled field
- **THEN** DataSpec recommends the field's current name
- **AND** the match reason and evidence identify that the input matched field history.

#### Scenario: Historical match points to a non-enabled field
- **WHEN** a historical value only matches a draft, deprecated, or disabled field
- **THEN** existing lifecycle filtering remains in effect
- **AND** DataSpec does not promote that historical value as a safe current recommendation.
