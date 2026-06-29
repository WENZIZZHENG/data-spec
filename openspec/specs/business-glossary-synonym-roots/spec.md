# business-glossary-synonym-roots Specification

## Purpose
Define DataSpec's project-scoped business glossary layer for AI-readable terms, synonyms, roots, abbreviations, disabled terms, canonical field hints, conflict diagnostics, field suggestion/search integration, AI Context export, and first-version frontend maintenance.

## Requirements
### Requirement: Manage project business glossary entries
DataSpec SHALL provide project-scoped business glossary entries for terms, synonyms, roots, abbreviations, disabled terms, canonical field hints, and examples.

#### Scenario: Create and list glossary entries
- **WHEN** a user creates an enabled glossary entry for project `P` with term `会员`, synonyms `用户,账号`, root terms `user,member`, abbreviation `hy`, and canonical field `user_id`
- **THEN** the entry SHALL be persisted under project `P`
- **AND** `GET /api/glossary?projectId=P` SHALL return the entry with normalized status and canonical field metadata.

#### Scenario: Update and soft-delete glossary entries
- **WHEN** a glossary entry is updated or deleted
- **THEN** DataSpec SHALL verify the entry belongs to the current project boundary
- **AND** delete SHALL soft-delete the entry so it no longer appears in normal list or AI Context export.

#### Scenario: Reject duplicate active term
- **WHEN** an active non-deleted glossary entry with the same normalized term already exists in the same project
- **THEN** creating or updating another active entry with that term SHALL fail with a readable business error.

### Requirement: Detect glossary conflicts
DataSpec SHALL detect glossary conflicts that would confuse AI field recommendation.

#### Scenario: Duplicate synonym conflict
- **WHEN** two enabled glossary entries in the same project share the same normalized synonym, root term, or abbreviation
- **THEN** the conflict API SHALL report the shared token, involved entries, severity, and suggested next action.

#### Scenario: Disabled term conflict
- **WHEN** a disabled term of one enabled entry is also used as a term, synonym, root, or abbreviation in another enabled entry
- **THEN** the conflict API SHALL report the disabled-term conflict.

#### Scenario: Missing canonical field warning
- **WHEN** an enabled entry references a missing, deleted, disabled, or cross-project canonical field
- **THEN** the conflict API SHALL report a warning without blocking other glossary reads.

### Requirement: Use glossary in field suggestion and search
DataSpec SHALL use enabled glossary entries to improve field suggestions and field search while preserving existing aliases and deterministic fallback behavior.

#### Scenario: Suggest fields from natural language glossary terms
- **WHEN** the project has enabled glossary entries connecting `会员` to `user_id` and `手机号/mobile` to `mobile_no`
- **AND** a user or AI requests suggestions for `会员手机号`
- **THEN** `FieldService.suggest` SHALL rank `mobile_no` and related canonical fields ahead of unrelated generic matches
- **AND** each glossary-assisted suggestion SHALL include a match reason containing `术语表`.

#### Scenario: Search expands glossary terms
- **WHEN** a user searches fields with query `订单费用`
- **AND** the project glossary maps `费用/price/amount` to amount-related standard fields
- **THEN** field search SHALL return matching amount fields even when the exact query text is not present in the field name
- **AND** the result SHALL include glossary match reasons and AI-readable next actions.

#### Scenario: Disabled terms do not become positive matches
- **WHEN** a query only matches a glossary disabled term
- **THEN** DataSpec SHALL not boost fields by that disabled term
- **AND** it SHALL include a warning or match hint explaining the term is disabled when relevant.

### Requirement: Export glossary in AI Context
DataSpec SHALL include a compact project glossary in AI Context exports.

#### Scenario: Export enabled glossary entries
- **WHEN** AI Context field catalog JSON is generated for a project with enabled glossary entries
- **THEN** the JSON SHALL include a top-level `glossary` array
- **AND** each item SHALL include term, synonyms, rootTerms, abbreviations, disabledTerms, canonicalFieldName, scopeType, scopeValue, and exampleFields when present.

#### Scenario: Bound glossary export size
- **WHEN** a project has more glossary entries than the export limit
- **THEN** AI Context SHALL export a bounded subset
- **AND** it SHALL include a warning that the glossary was truncated.

### Requirement: Provide frontend glossary maintenance page
DataSpec SHALL expose a first-version frontend page for maintaining project glossary entries.

#### Scenario: Manage glossary from current project
- **WHEN** a user opens the business glossary page with a current project selected
- **THEN** the page SHALL load glossary entries and available fields for that project
- **AND** the user SHALL be able to create, edit, disable, and delete entries through Element Plus forms.

#### Scenario: Show glossary conflicts
- **WHEN** the conflict API reports duplicate synonyms, disabled-term conflicts, or missing canonical fields
- **THEN** the frontend SHALL show the conflict summary and affected entries without blocking normal editing.
