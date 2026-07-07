# standard-change-what-if Specification

## Purpose
定义标准字段和规则变更的只读 what-if 预览，在保存前展示属性差异、影响摘要、风险等级、验证建议、回滚提示和当前快照信息。
## Requirements
### Requirement: Standard change what-if preview
DataSpec SHALL provide a read-only what-if preview for standard field and rule changes before the change is saved.

#### Scenario: Preview field update
- **WHEN** a caller previews a field update with the current project and target field
- **THEN** DataSpec returns changed attributes, impact summary, risk level, suggested validation commands, rollback hints, and current snapshot metadata.
- **AND** the preview does not modify fields, rules, templates, snapshots, or change logs.

#### Scenario: Preview rule update
- **WHEN** a caller previews a rule config update or toggle
- **THEN** DataSpec returns rule attribute changes and impacts for SQL lint, AI Context, and rule baseline behavior.

#### Scenario: No effective change
- **WHEN** the proposed payload does not change the target object
- **THEN** DataSpec returns an `INFO` preview with no required confirmation and an empty changes list.

### Requirement: AI-readable rollback guidance
Standard change previews SHALL include rollback guidance that can be read by the frontend, CLI, or AI agents.

#### Scenario: Field rollback hint
- **WHEN** a field preview has effective changes
- **THEN** the preview explains that the saved change can be inspected in field change logs and reverted from a compatible update log.

#### Scenario: Snapshot hint
- **WHEN** the project has a current standard snapshot
- **THEN** the preview includes that snapshot metadata and suggests creating a new snapshot after accepting meaningful standard changes.

