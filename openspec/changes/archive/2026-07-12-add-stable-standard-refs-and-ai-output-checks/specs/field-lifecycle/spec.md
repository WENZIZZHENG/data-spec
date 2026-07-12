## ADDED Requirements

### Requirement: Lifecycle metadata drives canonical reference resolution
Field lifecycle metadata SHALL participate in stable reference resolution and AI output post-checks.

#### Scenario: Deprecated field resolves to replacement
- **WHEN** a deprecated or disabled field has `replacementFieldId`
- **THEN** its `canonicalRef` SHALL point to the replacement field stableRef
- **AND** resolution and post-check results SHALL preserve the stale field stableRef as evidence.

#### Scenario: Deprecated field without replacement
- **WHEN** a deprecated or disabled field has no valid replacement
- **THEN** resolution SHALL keep its own stableRef as canonicalRef
- **AND** post-check SHALL report that human confirmation is required.
