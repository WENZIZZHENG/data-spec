## ADDED Requirements

### Requirement: Scoped Usage Example Export
Scoped AI Context export SHALL apply scope and query filters to usage examples.

#### Scenario: Field scope filters examples
- **WHEN** a caller exports AI Context with `scope=field` and a query that matches a subset of standard fields
- **THEN** `.dataspec/usage-examples.json` includes examples tied to those matched fields first
- **AND** unrelated field examples are omitted unless they are `GENERAL` examples matching the query.

#### Scenario: Limit truncates examples
- **WHEN** more enabled examples match than the usage example export limit
- **THEN** the export returns at most that limit
- **AND** the summary records that examples were truncated.

#### Scenario: Snapshot export remains stable
- **WHEN** a caller exports AI Context for a historical snapshot
- **THEN** usage examples remain project-scoped current metadata
- **AND** the manifest or usage examples file records that examples are not part of the snapshot payload.
