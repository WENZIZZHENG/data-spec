# field-impact-analysis Specification

## Purpose
TBD - created by archiving change add-field-impact-analysis. Update Purpose after archive.
## Requirements
### Requirement: Generate field impact report
The system SHALL generate a read-only impact report for a standard field in a project.

#### Scenario: Field impact report
- **WHEN** a user requests an impact report with a valid `projectId` and `fieldId`
- **THEN** the system returns the field identity, project identity, summary counts, impact items, and edit warnings.
- **AND** the report includes no writes to standard fields, templates, lint records, import sources, or snapshots.

#### Scenario: Field does not belong to project
- **WHEN** a user requests an impact report for a field outside the requested project
- **THEN** the system rejects the request with a business error.

### Requirement: Summarize known impact sources
The system SHALL summarize known project-local sources that reference or may reference the field.

#### Scenario: Template references
- **WHEN** the field is used by one or more table template fields
- **THEN** the report includes template impact items with template id, template name, field count, and impact severity.

#### Scenario: Import source references
- **WHEN** the field has reverse import source records
- **THEN** the report includes import source impact items with source table, source column, database context, and latest import time.

#### Scenario: Historical SQL check references
- **WHEN** recent SQL check records contain the field name or display name
- **THEN** the report includes possible historical SQL impact items and marks them as possible references.

#### Scenario: Snapshot and AI Context references
- **WHEN** the project has standard snapshots or AI Context exports that can include the field catalog
- **THEN** the report includes snapshot/context impact items with latest version, hash, or export timestamp when available.

### Requirement: Provide edit warnings
The system SHALL provide non-blocking edit warnings and what-if inputs for field changes likely to affect downstream outputs.

#### Scenario: Critical field attributes
- **WHEN** a report contains template, SQL check, import source, snapshot, or code set impacts
- **THEN** the report includes warnings for changes to field name, data type, status, code set, or sensitive flag.

#### Scenario: Field what-if preview reuses impact report
- **WHEN** DataSpec previews a field update
- **THEN** the preview uses the field impact report as the source for field impact items and warning attributes.

### Requirement: Frontend impact visibility
The frontend SHALL expose field impact analysis from the field library.

#### Scenario: View field impact
- **WHEN** a user opens a field impact entry from the field library
- **THEN** the frontend loads the report for the current project and displays summary counts, impact items, and warnings.

#### Scenario: Edit field with impacts
- **WHEN** a user edits a field that has impact warnings
- **THEN** the frontend displays a non-blocking warning before or during save so the user can continue intentionally.
