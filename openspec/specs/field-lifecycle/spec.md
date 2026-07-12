# field-lifecycle Specification

## Purpose
为标准字段提供个人/小团队优先的轻量生命周期契约，让 AI 能区分草稿、可用、废弃和停用字段，并在历史字段需要保留时读取结构化替代说明。
## Requirements
### Requirement: Field Lifecycle Metadata
Fields SHALL expose lightweight lifecycle metadata for AI and frontend users.

#### Scenario: Field carries lifecycle replacement metadata
- **WHEN** a field is created, updated, listed, exported to AI Context or serialized through OpenAPI
- **THEN** it SHALL support `status`, `replacementFieldId` and `replacementReason`
- **AND** valid statuses SHALL include `draft`, `enabled`, `deprecated` and `disabled`.

#### Scenario: Replacement field is project-safe
- **WHEN** a field references `replacementFieldId`
- **THEN** the replacement SHALL belong to the same project
- **AND** the field SHALL NOT reference itself as its replacement.

### Requirement: AI Recommendation Uses Lifecycle
AI-facing field recommendation and search SHALL respect lifecycle metadata.

#### Scenario: Default suggestion excludes non-enabled fields
- **WHEN** field suggestion runs without explicit lifecycle override
- **THEN** `draft`, `deprecated` and `disabled` fields SHALL NOT be returned as existing recommendations.

#### Scenario: Explicit search explains deprecated field usage
- **WHEN** field search explicitly filters a non-enabled status
- **THEN** matching items SHALL include lifecycle status and replacement guidance in recommended use or next actions.

### Requirement: Lifecycle Quality And Context
Quality scoring and AI Context SHALL surface lifecycle guidance.

#### Scenario: Quality scoring uses structured replacement
- **WHEN** a deprecated or disabled field has `replacementFieldId` or `replacementReason`
- **THEN** quality scoring SHALL NOT emit `deprecated_without_replacement` for that field.

#### Scenario: AI Context exports lifecycle fields
- **WHEN** AI Context exports a field catalog
- **THEN** each field SHALL include lifecycle status and any replacement field ID or replacement reason available.

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
