## ADDED Requirements

### Requirement: Historical field references come from auditable change records
DataSpec SHALL derive historical field names and aliases from project-scoped field change records without changing the field stableRef or inventing history that cannot be traced to a stored snapshot.

#### Scenario: Resolve a renamed field by historical name
- **WHEN** a caller resolves a field name that appears in an existing change-log snapshot for a current field in the selected project
- **THEN** DataSpec returns that field's current `stableRef` and `canonicalRef`
- **AND** `matchedAlias` identifies the historical value and `evidenceLinks` includes the field and source change-log references.

#### Scenario: Historical name is ambiguous
- **WHEN** the same normalized historical name is traced to more than one current field in the selected project
- **THEN** DataSpec returns `AMBIGUOUS` without selecting a canonicalRef
- **AND** the response contains only project-scoped, secret-safe candidate evidence.

#### Scenario: Change history cannot be parsed
- **WHEN** a field change record has missing, malformed, or unsupported snapshot content
- **THEN** DataSpec ignores that record for historical-name matching
- **AND** current names, current aliases, and stableRef resolution continue to work.
