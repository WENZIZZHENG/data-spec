## ADDED Requirements

### Requirement: AI Context exports table standards
The AI Context package SHALL export project table structure standards in a dedicated machine-readable file.

#### Scenario: Package contains table standards file
- **WHEN** an AI Context zip is generated for a project
- **THEN** it contains `.dataspec/table-standards.json`
- **AND** that file includes `kind`, `schemaVersion`, `projectId`, `contextScope`, `businessObjects`, `templates`, `relations`, and `summary`.

#### Scenario: Field catalog remains compatible
- **WHEN** `.dataspec/table-standards.json` is added to the package
- **THEN** existing `.dataspec/field-catalog.json`, `.dataspec/rules.yaml`, `.dataspec/manifest.json`, and `AGENTS.md.fragment` fields remain present
- **AND** clients that ignore table standards can continue using the existing package layout.

#### Scenario: Database rules mention table standards
- **WHEN** `DATABASE_RULES.md` is generated for a project with business object or table structure standards
- **THEN** it includes a concise table structure section with primary key, unique key, index, foreign key, audit, soft delete, relation, and common pitfall guidance
- **AND** it tells AI clients not to execute generated DDL without human confirmation.

### Requirement: AI Context table-standard scoping
AI Context export SHALL support table-standard scoping without changing existing field scoping semantics.

#### Scenario: Export by business object
- **WHEN** a caller exports AI Context with scope `business-object` and a query matching an object key, entity name, or table pattern
- **THEN** `.dataspec/table-standards.json` includes matching objects, related templates, relation edges, and referenced fields
- **AND** `contextScope` records matched count, returned count, warnings, and truncation status.

#### Scenario: Export by table template
- **WHEN** a caller exports AI Context with scope `table-template` and a query matching a template name or table pattern
- **THEN** `.dataspec/table-standards.json` includes the matching templates, structure standards, related business objects, and relation hints
- **AND** field catalog scoping remains additive and does not hide required table-standard fields without a warning.

#### Scenario: No table standards remain valid
- **WHEN** a project has no business object or table structure standards
- **THEN** the package still contains a valid `.dataspec/table-standards.json` with empty arrays and a summary
- **AND** no noisy empty sections are added to human-readable guidance.
