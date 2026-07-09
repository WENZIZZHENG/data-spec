# field-quality-scoring Specification

## Purpose
定义标准字段质量评分报告，用确定性规则评估 metadata 完整度、AI 复用风险、敏感标记缺口、码表缺失和命名问题。
## Requirements
### Requirement: Generate field quality report
The system SHALL generate a read-only quality report for standard fields in a project.

#### Scenario: Project quality report
- **WHEN** a user requests a field quality report with a valid `projectId`
- **THEN** the system returns a summary with total field count, average score, low quality count, issue counts by severity, and field quality items.
- **AND** each field item includes field id, name, display name, data type, score, level, issues, and suggestions.

#### Scenario: Empty project
- **WHEN** a project has no standard fields
- **THEN** the system returns zero counts, an average score of 0, and an empty field item list.

### Requirement: Score field metadata completeness
The system SHALL score field quality using deterministic metadata checks.

#### Scenario: Complete field receives high score
- **WHEN** a field has name, data type, display name, comment, aliases, category or tags, example value, and required semantic markers
- **THEN** the field receives a `GOOD` quality level and no blocking issue.

#### Scenario: Incomplete field receives issues
- **WHEN** a field lacks comment, aliases, example value, category or tags
- **THEN** the field quality item includes machine-readable issues with severity, code, message, and suggested action.
- **AND** the score is reduced according to the issue severities.

### Requirement: Detect AI-risk metadata gaps
The system SHALL highlight metadata gaps that can mislead AI field reuse.

#### Scenario: Sensitive-looking field not marked sensitive
- **WHEN** a field name, display name, comment, aliases, category, or tags indicate phone, email, id card, password, token, address, or similar sensitive concepts
- **THEN** the report includes a `sensitive_not_marked` issue unless the field is already marked sensitive.

#### Scenario: Status or enum field without code set
- **WHEN** a field looks like a status, type, category, kind, flag, level, or enum field
- **AND** it has no `codeSetId`
- **THEN** the report includes a `code_set_missing` issue.

#### Scenario: Deprecated field without replacement guidance
- **WHEN** a field status is `deprecated` or `disabled`
- **AND** comment, aliases, or tags do not include replacement or migration guidance
- **THEN** the report includes a `deprecated_without_replacement` issue.

#### Scenario: Format-sensitive field lacks format examples
- **WHEN** a field looks like an amount, phone, email, timestamp, date, JSON, status, enum, or code field
- **AND** the field lacks both structured format constraints and valid examples
- **THEN** the report includes a `format_examples_missing` issue with a machine-readable suggested action.

### Requirement: Frontend quality view
The frontend SHALL provide a project-scoped field quality view.

#### Scenario: View quality summary and low quality fields
- **WHEN** a user opens the field quality page with a current project selected
- **THEN** the page loads the report, displays quality summary cards, and lists fields sorted by score ascending by default.

#### Scenario: Filter and edit field
- **WHEN** a user filters by quality level or issue code
- **THEN** the table only shows matching field quality items.
- **AND** when the user chooses to edit a field, the frontend navigates to the field library with that field query or identifier.

### Requirement: Field quality report can seed maintenance workflow plans
DataSpec SHALL allow field quality report findings to seed standard maintenance workflow plans for metadata repair.

#### Scenario: Low quality fields create repair workflow
- **WHEN** a caller requests a maintenance workflow from field quality findings
- **THEN** DataSpec groups selected low quality fields, issue codes, severity, and suggested actions into dry-run repair steps
- **AND** the plan links verification back to the field quality report or quality gate check.

#### Scenario: Quality workflow preserves report-only semantics
- **WHEN** a quality maintenance workflow is generated
- **THEN** DataSpec does not modify field comments, aliases, examples, sensitivity markers, code sets, or status.
