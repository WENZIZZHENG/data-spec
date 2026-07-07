# field-bulk-maintenance Specification

## Purpose
定义标准字段批量维护的预览、确认应用和撤销边界，让用户批量修改状态、分类、标签、敏感标记、码表和别名时可先看差异并保留变更日志。
## Requirements
### Requirement: Field bulk update preview
The system SHALL provide a read-only preview before applying bulk field maintenance updates.

#### Scenario: Preview selected field changes
- **WHEN** a user selects fields in one project and submits bulk updates for supported attributes
- **THEN** the system returns the requested count, changed count, skipped unchanged count, and per-field before/after changes without writing any field or change log.

#### Scenario: Reject unsupported bulk update keys
- **WHEN** a preview request contains update keys outside `status`, `category`, `tags`, `sensitive`, `codeSetId`, or `aliases`
- **THEN** the system rejects the request with a business error and does not access unrelated project data.

### Requirement: Field bulk update commit
The system SHALL allow users to batch update common field metadata for selected fields in the same project.

#### Scenario: Apply bulk maintenance updates
- **WHEN** a user confirms a preview for selected fields and submits the same updates
- **THEN** the system applies only changed supported attributes and records one standard change log per changed field.

#### Scenario: Reject cross-project bulk update
- **WHEN** a bulk update request contains field IDs that do not belong to the requested project
- **THEN** the system rejects the entire request without partially updating any field.

#### Scenario: Skip unchanged fields
- **WHEN** the requested updates do not change a selected field after normalization
- **THEN** the system leaves that field untouched and does not write a no-op change log for it.

### Requirement: Field change undo
The system SHALL allow users to restore a field from a recent field update change log.

#### Scenario: Undo field update log
- **WHEN** a user requests undo for a field update log that belongs to the current project and contains a valid before snapshot
- **THEN** the system restores the field editable attributes from the before snapshot and records a new undo change log.

#### Scenario: Reject unsafe undo
- **WHEN** the change log belongs to another project, targets another field, lacks a before snapshot, or would violate field name uniqueness
- **THEN** the system rejects the undo request without modifying the field.

### Requirement: Field library bulk maintenance experience
The frontend SHALL let users preview, apply, and undo field maintenance changes from the field library.

#### Scenario: Preview and apply from selected rows
- **WHEN** a project is selected and a user selects rows in the field library
- **THEN** the page offers a bulk maintenance dialog, displays preview changes before submit, applies the update after confirmation, and refreshes the table and grouping summary.

#### Scenario: Undo recent field change
- **WHEN** a user opens a field's recent change log from the field library
- **THEN** the page shows undo-capable update logs and lets the user restore a previous version after confirmation.
