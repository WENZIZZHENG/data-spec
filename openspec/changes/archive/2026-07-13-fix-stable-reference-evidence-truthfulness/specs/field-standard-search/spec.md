## ADDED Requirements

### Requirement: Field search returns auditable historical-name matches
Field standard search SHALL use project-scoped field change records as a deterministic historical-name source while keeping current names and current aliases higher priority.

#### Scenario: Search by a previous field name
- **WHEN** a query matches a historical field name or alias from an existing change-log snapshot
- **THEN** search returns the current field with its current `stableRef` and `canonicalRef`
- **AND** `matchedAlias`, match reasons, and evidence identify the historical value and source change log.

#### Scenario: Current name competes with historical name
- **WHEN** one field matches the current name or alias and another field only matches a historical value
- **THEN** the current-name or current-alias match ranks above the historical-only match when other scoring inputs are equal.
