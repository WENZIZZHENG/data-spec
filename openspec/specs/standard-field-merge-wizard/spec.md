# standard-field-merge-wizard Specification

## Purpose
TBD - created by archiving change add-standard-field-merge-wizard. Update Purpose after archive.
## Requirements
### Requirement: 标准字段合并预览
DataSpec SHALL provide a merge preview before changing formal standard fields.

#### Scenario: 生成单来源字段合并预览
- **WHEN** a user submits a project id, target field id, and source field id from the same project
- **THEN** DataSpec SHALL return a merge preview with `kind`, `schemaVersion`, target field summary, source field summary, proposed target after-state, proposed source after-state, changes, risks, impact items, rollback hints, and next actions
- **AND** the preview SHALL identify which aliases, tags, examples, format notes, and source summaries are safe to migrate or only safe to review.

#### Scenario: 阻断不安全预览
- **WHEN** the target field and source field belong to different projects, are the same field, or the source field is already merged into another replacement
- **THEN** DataSpec SHALL reject the request with a stable validation error
- **AND** it SHALL NOT update any field or change log.

### Requirement: 标准字段合并确认应用
DataSpec SHALL apply a merge only after an explicit confirmation with reason.

#### Scenario: 应用合并计划
- **WHEN** a user confirms a merge preview with a non-empty reason
- **THEN** DataSpec SHALL update the target field with safe merged aliases and tags
- **AND** it SHALL mark the source field as `deprecated`
- **AND** it SHALL set `replacementFieldId` to the target field id
- **AND** it SHALL set `replacementReason` with the user reason and merge summary
- **AND** it SHALL record standard change logs for both affected fields.

#### Scenario: 保留不可自动迁移属性
- **WHEN** source and target fields disagree on data type, nullable, code set, sensitive flag, lifecycle, or format constraints
- **THEN** DataSpec SHALL report these as risks or manual review items
- **AND** it SHALL NOT silently overwrite the target field's authoritative attributes.

### Requirement: 合并向导前端体验
DataSpec SHALL provide a user-visible merge wizard for standard fields.

#### Scenario: 从字段冲突进入合并向导
- **WHEN** a conflict group contains two or more fields
- **THEN** the frontend SHALL let the user choose target and source fields, request preview, review changes and risks, enter a merge reason, and apply the merge
- **AND** completed merge results SHALL show rollback hints and links back to the affected fields.

#### Scenario: 前端阻止缺少确认理由的 apply
- **WHEN** the merge reason is blank
- **THEN** the frontend SHALL block apply and tell the user to provide a merge reason
- **AND** it SHALL NOT call the apply API.

### Requirement: AI 可读合并契约
DataSpec SHALL expose merge preview and result objects that AI can safely consume.

#### Scenario: 合并响应不泄漏敏感信息
- **WHEN** DataSpec returns merge preview or merge result
- **THEN** the response SHALL NOT contain password, token, Authorization, JDBC URL, DSN, or source database row values
- **AND** it SHALL include `nextActions` and `rollbackHints` so AI can explain the merge and recovery path.
