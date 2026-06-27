## ADDED Requirements

### Requirement: Field grouping summary
The system SHALL provide a project-level field grouping summary based on existing field metadata.

#### Scenario: Group fields by domain category and tag
- **WHEN** a user requests grouping summary for a project with fields containing `domainId`, `category`, and `tags`
- **THEN** the system returns groups for domain, category, tag, and ungrouped fields with counts and sample field names.

#### Scenario: Detect ungrouped fields
- **WHEN** fields have no `domainId`, empty `category`, and empty `tags`
- **THEN** the summary marks them as ungrouped and returns the ungrouped count.

### Requirement: Batch update field grouping metadata
The system SHALL allow users to batch update grouping metadata for selected fields in the same project.

#### Scenario: Batch assign category and tags
- **WHEN** a user selects multiple fields and submits `category` and `tags` updates
- **THEN** the system updates only those grouping fields for the selected fields and records standard change logs.

#### Scenario: Reject cross-project batch update
- **WHEN** a batch update request contains field IDs that do not belong to the requested project
- **THEN** the system rejects the request without partially updating unrelated project fields.

### Requirement: Field library grouping experience
The frontend SHALL let users browse and maintain fields by grouping dimensions.

#### Scenario: Browse grouped fields
- **WHEN** a project is selected on the field library page
- **THEN** the page shows a grouping panel with domain, category, tag, and ungrouped counts and lets the user filter the field table by a group.

#### Scenario: Batch assign selected fields
- **WHEN** a user selects fields in the field table and chooses batch grouping
- **THEN** the page displays a confirmation dialog, submits the batch update, refreshes the grouping summary, and keeps the current project context.

### Requirement: AI Context includes grouping summary
AI Context exports SHALL include optional grouping summary metadata for scoped field catalogs.

#### Scenario: Export scoped field catalog
- **WHEN** an AI Context field catalog is exported with scope, query, status, or limit filters
- **THEN** the resulting context metadata includes group counts for the returned fields and warns when ungrouped fields are present.

#### Scenario: Backward compatibility
- **WHEN** existing AI tools read `field-catalog.json`
- **THEN** existing required fields remain present and grouping summary appears only as additive optional metadata.
