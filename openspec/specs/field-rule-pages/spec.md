# field-rule-pages Specification

## Purpose
定义字段库和规则配置前端页面的核心操作边界，支持在当前项目下维护标准字段、个人 metadata、规则配置和基线操作。
## Requirements
### Requirement: Field Library Page
The system SHALL provide a usable frontend page for managing standard fields in the current project.

#### Scenario: Manage fields for selected project
- **WHEN** a user opens the field library page with a current project selected
- **THEN** the page loads fields for that project
- **AND** the user can create, edit, and delete fields with the backend field API

#### Scenario: Field form supports personal metadata
- **WHEN** a user creates or edits a field
- **THEN** the form supports aliases, category, sensitive flag, status, and example value

### Requirement: Rule Config Page
The system SHALL provide a usable frontend page for managing lint rule configs in the current project.

#### Scenario: Manage rules for selected project
- **WHEN** a user opens the rule config page with a current project selected
- **THEN** the page loads rule configs for that project
- **AND** the user can create, edit, delete, enable, and disable rule configs with the backend rule API

#### Scenario: Rule params remain structured text
- **WHEN** a user edits rule params
- **THEN** the page preserves `paramsJson` as editable JSON text without adding a separate rule DSL

### Requirement: Current Project Boundary
The system SHALL require a selected current project before field or rule changes are submitted.

#### Scenario: No project selected
- **WHEN** no current project is selected
- **THEN** the field library and rule config pages show an actionable empty state instead of submitting API calls without `projectId`

### Requirement: Rule baseline respects current project boundary
The rule config page SHALL require a selected current project before baseline operations are submitted.

#### Scenario: No project selected for baseline action
- **WHEN** no current project is selected
- **THEN** baseline list can be viewed if available
- **AND** apply, import, and export actions are disabled or show an actionable empty state
- **AND** no API call is submitted without `projectId`
