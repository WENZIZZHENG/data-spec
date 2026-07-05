# standard-reuse-pack Specification

## Purpose
TBD - created by archiving change add-standard-reuse-pack. Update Purpose after archive.
## Requirements
### Requirement: 标准复用包创建与列表
DataSpec SHALL allow users to create and list versioned standard reuse packs from a source project.

#### Scenario: 从源项目创建复用包
- **WHEN** a user creates a standard reuse pack for a source project with `packKey`, `packName`, and `basePackVersion`
- **THEN** DataSpec SHALL persist a package with deterministic payload JSON, asset counts, `packageHash`, source project summary, included fields, rules, templates, and enum definitions
- **AND** the package SHALL NOT contain API token plaintext, token hashes, database passwords, complete JDBC URLs, DSNs, Authorization headers, or source database rows.

#### Scenario: 列出项目复用包
- **WHEN** a caller lists standard reuse packs for a source project
- **THEN** DataSpec SHALL return only packs belonging to that project
- **AND** each row SHALL include `packId`, `packKey`, `packName`, `basePackVersion`, `packageHash`, asset counts, and created time.

### Requirement: 标准复用包应用预览
DataSpec SHALL provide a dry-run apply preview before writing standard reuse pack assets into a target project.

#### Scenario: 预览包应用
- **WHEN** a user previews applying a valid standard reuse pack to a target project
- **THEN** DataSpec SHALL compare package assets using project-scoped natural keys
- **AND** it SHALL return created, skipped, overridden, drifted, and blocked counts
- **AND** it SHALL return item-level actions without writing fields, enums, rules, templates, or application records.

#### Scenario: 拒绝无效预览
- **WHEN** the pack is missing, the target project is missing, the package hash is invalid, or the package schema version is unsupported
- **THEN** DataSpec SHALL reject the preview with a readable validation error
- **AND** no project assets SHALL be written.

### Requirement: 标准复用包确认应用
DataSpec SHALL apply a standard reuse pack only after explicit confirmation.

#### Scenario: 应用缺失资产
- **WHEN** a user confirms applying a valid standard reuse pack to a target project
- **THEN** DataSpec SHALL create missing domains, enum dictionaries and values, fields, rule configs, templates, and template fields in dependency order
- **AND** fields created from the pack SHALL carry a `pack:<packKey>@<basePackVersion>` source marker
- **AND** existing target assets SHALL NOT be overwritten or deleted by default.

#### Scenario: 记录应用摘要
- **WHEN** a standard reuse pack apply completes
- **THEN** DataSpec SHALL store a target project application summary with pack key, pack name, base pack version, package hash, source project, created counts, skipped counts, drift counts, drift report, operator, and applied time
- **AND** the record SHALL NOT store raw secrets or source database rows.

### Requirement: 标准复用包漂移报告
DataSpec SHALL report how a target project differs from an applied or selected standard reuse pack.

#### Scenario: 计算漂移
- **WHEN** a user requests drift for a target project and a standard reuse pack
- **THEN** DataSpec SHALL report matched, missing, overridden, and drifted items across fields, enums, rules, and templates
- **AND** each item SHALL include asset type, natural key, action, reason, and optional pack/target summaries safe for AI consumption.

#### Scenario: 共享包升级后识别差异
- **WHEN** a source project creates a newer pack version and a target project compares against it
- **THEN** DataSpec SHALL surface package assets that are missing or different in the target project
- **AND** existing target overrides SHALL remain visible instead of being silently replaced.

### Requirement: 标准复用包前端体验
DataSpec Web SHALL provide a project-scoped standard reuse pack workflow.

#### Scenario: 创建并查看复用包
- **WHEN** a user opens the standard reuse pack page with a selected project
- **THEN** the page SHALL allow creating a pack from the current project and list existing packs with version, hash, counts, and created time.

#### Scenario: 预览并应用到当前项目
- **WHEN** a user selects a pack and target project
- **THEN** the page SHALL show dry-run counts and drift details before enabling apply
- **AND** after apply it SHALL show the application summary and latest drift status.

### Requirement: AI Context 标准包来源说明
DataSpec SHALL export standard reuse pack source metadata in AI Context in an additive, backward-compatible way.

#### Scenario: 字段目录说明共享包来源
- **WHEN** AI Context field catalog is generated for a project with fields created from a standard reuse pack
- **THEN** each matching field SHALL include `standardPackSources` with `packKey` and `basePackVersion`
- **AND** existing field properties SHALL remain present and compatible.

#### Scenario: Manifest 说明项目标准包应用摘要
- **WHEN** AI Context manifest is generated for a project with standard reuse pack application records
- **THEN** the manifest SHALL include a `standardPacks` summary with recent pack applications and drift counts
- **AND** projects without applications SHALL remain valid by omitting the property or returning an empty array.
