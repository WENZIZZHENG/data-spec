## ADDED Requirements

### Requirement: Field responses include stable reference metadata
Standard field responses and AI exports SHALL expose additive stable reference metadata.

#### Scenario: Export stable field metadata
- **WHEN** a field is listed, searched, resolved, or exported to AI Context
- **THEN** it SHALL include `stableRef` and `canonicalRef`
- **AND** it SHALL include `aliasHistory` and `deprecatedRefs` when those values can be derived from current aliases, lifecycle replacement, change logs, or snapshots.

#### Scenario: Existing field clients remain compatible
- **WHEN** stable reference metadata is added
- **THEN** existing field IDs, names, aliases, status, and usage contract fields SHALL keep their documented semantics.
