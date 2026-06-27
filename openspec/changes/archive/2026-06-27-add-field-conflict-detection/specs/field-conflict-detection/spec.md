## ADDED Requirements

### Requirement: Generate field conflict report
The system SHALL generate a read-only field conflict report for a project.

#### Scenario: Conflict report
- **WHEN** a user requests a conflict report for a valid `projectId`
- **THEN** the system returns summary counts and conflict groups
- **AND** the report does not modify fields, aliases, code sets, snapshots, or rules.

### Requirement: Detect duplicate aliases and semantic conflicts
The system SHALL detect common duplicate and conflicting standard field definitions.

#### Scenario: Alias conflict
- **WHEN** the same alias belongs to multiple fields or an alias equals another field name
- **THEN** the report includes an alias conflict group with involved fields and evidence.

#### Scenario: Semantic duplicate
- **WHEN** multiple fields belong to the same known semantic group such as user id, mobile number, amount, ID card, or order number
- **THEN** the report includes a semantic duplicate group.

#### Scenario: Attribute mismatch
- **WHEN** fields in the same conflict group have different data types, code sets, sensitive flags, or statuses
- **THEN** the report marks the group with a warning or error severity and includes mismatch evidence.

### Requirement: Frontend conflict visibility
The frontend SHALL expose field conflict detection from a dedicated page.

#### Scenario: View field conflicts
- **WHEN** a user opens the field conflict page for the current project
- **THEN** the frontend displays summary counts, filter controls, conflict groups, involved fields, evidence, and suggested actions.

#### Scenario: Edit involved field
- **WHEN** a user clicks an involved field
- **THEN** the frontend routes to the field library with that field selected for editing.
